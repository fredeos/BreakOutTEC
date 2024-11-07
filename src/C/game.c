#include <gtk/gtk.h>
#include <json-c/json.h>

#include <stdlib.h>
#include <time.h>
#include <math.h>
#include <stdio.h>
#include <string.h>

#include "modules/network/socket.h"


// ---------------------------------------------------------------------------------------------------------------------------------------
// Bloque de constantes del juego
// ---------------------------------------------------------------------------------------------------------------------------------------
#define WINDOW_WIDTH 600
#define WINDOW_HEIGHT 600
#define PLATFORM_WIDTH 80
#define PLATFORM_HEIGHT 10
#define BALL_SIZE 10
#define BRICK_WIDTH 60
#define BRICK_HEIGHT 20
#define MAX_ROWS 4
#define MAX_COLUMNS 8
#define DEFAULT_BALL_SPEED 4
#define DEFAULT_PLATFORM_SPEED 4
#define DEFAULT_LIFE 3
#define BALL_LIMIT 10

// ---------------------------------------------------------------------------------------------------------------------------------------
// Bloque de estructuras del jueg
// ---------------------------------------------------------------------------------------------------------------------------------------
typedef struct {
    int x, y;
    int dx, dy;
    gboolean activa;
    int id;
} Ball;

typedef struct {
    int x, y;
    int ancho, alto;
    gboolean visible;
    int tipo; // 0 = normal, 1 = vida extra, 2 = bola extra, 3 = raqueta doble, 4 = raqueta mitad, 5 = velocidad +
    int num_color;
    int mod;
} Brick;

typedef struct {
    GtkWidget *widget1;
    GtkWidget *widget2;
    GtkWidget *widget3;
    GtkWidget *widget4;
    GtkWidget *widget5;
} Bundle;

// ---------------------------------------------------------------------------------------------------------------------------------------
// Bloque de elementos para los sockets
// ---------------------------------------------------------------------------------------------------------------------------------------

enum client_type {
    PLAYER,
    SPECTATOR
};

static int online = 1;
static int spectator_selection = 0;
static enum client_type ct;
static int port;
static char* ip;
static char* client_name;

static struct client* client;

// ---------------------------------------------------------------------------------------------------------------------------------------
// Bloque de elementos y variables globales(valores iniciales) para el juego
// ---------------------------------------------------------------------------------------------------------------------------------------

static GtkWidget *DrawArea;
static int platform_x_coord = (WINDOW_WIDTH - PLATFORM_WIDTH) / 2;
static int platform_width = PLATFORM_WIDTH;
static int life = DEFAULT_LIFE;
static int ball_speed = DEFAULT_BALL_SPEED;
static int active_balls = 1;
static Ball balls[BALL_LIMIT];
static Brick bricks[MAX_ROWS*MAX_COLUMNS];
static int score = 0;


// ---------------------------------------------------------------------------------------------------------------------------------------
// Funciones adicionales para los sockets en el juego
// ---------------------------------------------------------------------------------------------------------------------------------------

void update_post(struct json_object* out_json){
    const char* str = json_object_get_string(out_json);
    size_t len = strlen(str);
    char* out_str = malloc(len+2);
    strcpy(out_str,str);
        out_str[len] = '\n';
        out_str[len+1] = '\0';
    client->OUTbuffer = out_str;
}

void clear_tray(){
    free(client->OUTbuffer);
    memset(client->INbuffer,0,sizeof(client->INbuffer));
}

// ---------------------------------------------------------------------------------------------------------------------------------------
// Funciones generales del juego
// ---------------------------------------------------------------------------------------------------------------------------------------

/* */
int determinePower(struct json_object* powerup){
    if (strcmp(json_object_get_string(powerup),"none")==0){
        return 0;
    }
    const char* category = json_object_get_string(json_object_object_get(powerup,"category"));
    const char* modifier_type = json_object_get_string(json_object_object_get(powerup,"modifier"));
    if (strcmp(category,"ball")==0){
        if (strcmp(modifier_type,"speed")==0){
            return 5;
        } else if (strcmp(modifier_type,"quantity")==0){
            return 2;
        }
    } else if (strcmp(category,"racket")==0){
        int value = json_object_get_int(json_object_object_get(powerup,"value"));
        if (strcmp(modifier_type,"speed")==0){
            return 5;
        } else if (strcmp(modifier_type,"size")==0){
            if (value > 0){
                return 3;
            } else {
                return 4;
            }
        }
    } else if (strcmp(category,"life")==0){
        if (strcmp(modifier_type,"quantity")==0){
            return 1;
        }
    }
    return 0;
}

/* */
int determineColor(const char* color){
    int colorcode = 0;
    if (strcmp(color,"green")==0){
        colorcode = 1;
    } else if (strcmp(color,"yellow")==0){
        colorcode = 2;
    } else if (strcmp(color,"orange")==0){
        colorcode = 3;
    } else if (strcmp(color,"red")==0){
        colorcode = 4;
    }
    return colorcode;
}

/* */
void onExitEvent(GtkWidget *widget, GdkEvent *event, gpointer data){
    struct json_object* out_json = json_object_new_object();
    json_object_object_add(out_json,"id",json_object_new_string(client->uuid));
    json_object_object_add(out_json,"name",json_object_new_string(client_name));
    json_object_object_add(out_json, "request", json_object_new_string("end-connection"));
        update_post(out_json);
        send_message(client);
    json_object_put(out_json);
    close(client->sock_container.socket);
    free(client);
    gtk_main_quit();
}

/* */
gboolean drawOnEvent(GtkWidget *widget, cairo_t *cr, gpointer data) {
    cairo_set_source_rgb(cr, 0.0, 0.0, 0.0);
    cairo_paint(cr);

    // Dibujar plataforma
    cairo_set_source_rgb(cr, 0.0, 1.0, 0.0);
    cairo_rectangle(cr, platform_x_coord, WINDOW_HEIGHT - PLATFORM_HEIGHT - 10, platform_width, PLATFORM_HEIGHT);
    cairo_fill(cr);

    // Dibujar las bolas activas
    for (int i = 0; i < BALL_LIMIT; i++) {
        if (balls[i].activa) {
            cairo_set_source_rgb(cr, 1.0, 0.0, 0.0);
            cairo_arc(cr, balls[i].x, balls[i].y, BALL_SIZE, 0, 2 * G_PI);
            cairo_fill(cr);
        }
    }

    // Dibujar bloques de acuerdo a su tipo y color
    for (int i = 0; i < MAX_COLUMNS*MAX_ROWS; i++) {
        if (bricks[i].visible) {
            switch (bricks[i].num_color) {
                case 0: cairo_set_source_rgb(cr, 0.8, 0.8, 0.8); break; // Normal
                case 1: cairo_set_source_rgb(cr, 0.0, 1.0, 0.0); break; // Vida extra
                case 2: cairo_set_source_rgb(cr, 1.0, 1.0, 0.0); break; // Bola extra
                case 3: cairo_set_source_rgb(cr, 1.0, 0.5, 0.0); break; // Raqueta doble
                case 4: cairo_set_source_rgb(cr, 1.0, 0.0, 0.0); break; // Raqueta mitad
                case 5: cairo_set_source_rgb(cr, 0.5, 0.0, 0.5); break; // Velocidad +
            }
            cairo_rectangle(cr, bricks[i].x, bricks[i].y, bricks[i].ancho, bricks[i].alto);
            cairo_fill(cr);
        }
    }

    return FALSE;
}

// ---------------------------------------------------------------------------------------------------------------------------------------
// Funciones del JUGADOR
// ---------------------------------------------------------------------------------------------------------------------------------------

void initiateByResponse(struct json_object* contents){
    // Inicializar bloques
    int counter = 0;
    struct json_object* bricks_list = json_object_object_get(contents,"bricks");
    for (int i = MAX_ROWS-1; i >= 0; i--) {
        struct json_object* layer = json_object_array_get_idx(bricks_list,i);
        for (int j = MAX_COLUMNS-1; j >= 0; j--){
            struct json_object* brick = json_object_array_get_idx(layer,j);
            struct json_object* power = json_object_object_get(brick,"powerup");
            printf("Brick: %s\n",json_object_get_string(brick));
            bricks[counter].x = (counter % 8) * (BRICK_WIDTH + 5) + 10;
            bricks[counter].y = (counter / 8) * (BRICK_HEIGHT + 5) + 10;
            bricks[counter].ancho = BRICK_WIDTH;
            bricks[counter].alto = BRICK_HEIGHT;
            if (json_object_get_int(json_object_object_get(brick,"durability"))==0){
                bricks[counter].visible = FALSE;
            } else {
                bricks[counter].visible = TRUE;
            }
            bricks[counter].tipo = determinePower(power);
            bricks[counter].num_color = determineColor(json_object_get_string(json_object_object_get(brick,"color")));
            if (strcmp(json_object_get_string(power),"none")!=0){
                bricks[counter].mod = json_object_get_int(json_object_object_get(power,"value"));
            } else {
                bricks[counter].mod = 0;
            }
            counter++;
        }
    }
    // Inicializar bolas
    struct json_object* balls_list = json_object_object_get(contents,"balls");
    for(int i = 0; i < json_object_array_length(balls_list); i++){
        struct json_object* ball = json_object_array_get_idx(balls_list,i);
        struct json_object* position = json_object_object_get(ball,"position");
        printf("Ball: %s\n",json_object_get_string(ball));

        double x = (json_object_get_double(json_object_array_get_idx(position,0))/100)*WINDOW_WIDTH;
        double y = (json_object_get_double(json_object_array_get_idx(position,1))/100)*WINDOW_HEIGHT;
        int speed_mult = json_object_get_int(json_object_object_get(ball,"speed"));
        
        balls[i].x = (int)x;
        balls[i].y = (int)y;
        balls[i].dx = DEFAULT_BALL_SPEED*speed_mult;
        balls[i].dy = DEFAULT_BALL_SPEED*speed_mult,
        balls[i].activa = TRUE;
        balls[i].id = json_object_get_int(json_object_object_get(ball,"id"));
    }
    // Inicializar plataforma
    struct json_object* platform = json_object_object_get(contents,"racket");
    printf("Racket: %s\n",json_object_get_string(platform));

    double x = (json_object_get_double(json_object_object_get(platform,"position"))/100)*WINDOW_WIDTH;
    platform_x_coord = (int)x;
    platform_width = PLATFORM_WIDTH*json_object_get_int(json_object_object_get(platform,"size"));

    memset(client->INbuffer,0,sizeof(client->INbuffer));
}

/* */
void playerUpdateFromResponse(){
    struct json_object* in_json = json_tokener_parse(client->INbuffer);
    const char* response = json_object_get_string(json_object_object_get(in_json,"response"));
    if (strcmp(response,"session-updated")==0){
        int did_server_updated = json_object_get_boolean(json_object_object_get(in_json,"server-updated"));
        if (did_server_updated == 1){
            struct json_object* new_info = json_object_object_get(in_json, "server-changes");
            struct json_object* position = json_object_object_get(new_info, "position");
            struct json_object* powerup = json_object_object_get(new_info, "powerup");

            int i = json_object_get_int(json_object_array_get_idx(position,0));
            int j = json_object_get_int(json_object_array_get_idx(position,1));
            int index = MAX_COLUMNS*i + j;
            printf("Bloque[%d][%d](%d): modificado con (%s)",i,j,index,json_object_get_string(powerup));

            bricks[index].tipo = determinePower(powerup);
            bricks[index].mod = json_object_get_int(json_object_object_get(powerup,"value"));
        }
    }
    json_object_put(in_json);
    clear_tray();
}

/* */
void brickCollision(int index){
    // Buscar bloque y definir su posicion en matriz
    int i = index/MAX_COLUMNS;
    int j = index - i*MAX_COLUMNS;
    Brick brick = bricks[index];
    // Parsear bloque
    struct json_object* position = json_object_new_array();
        json_object_array_add(position, json_object_new_int(i));
        json_object_array_add(position, json_object_new_int(j));
    struct json_object* brick_object = json_object_new_object();
        json_object_object_add(brick_object,"durability",json_object_new_int(0));
        json_object_object_add(brick_object,"position",position);
        switch(brick.tipo){
            case 1:
                json_object_object_add(brick_object,"color",json_object_new_string("green"));
                break;
            case 2:
                json_object_object_add(brick_object,"color",json_object_new_string("yellow"));
                break;
            case 3:
                json_object_object_add(brick_object,"color",json_object_new_string("orange"));
                break;
            case 4:
                json_object_object_add(brick_object,"color",json_object_new_string("red"));
                break;
        }
    // Enviar mensaje
    struct json_object* out_json = json_object_new_object();
    json_object_object_add(out_json,"id",json_object_new_string(client->uuid));
    json_object_object_add(out_json,"name",json_object_new_string(client_name));
    json_object_object_add(out_json,"score",json_object_new_int(score));
    json_object_object_add(out_json,"request",json_object_new_string("update-game"));
    json_object_object_add(out_json,"action",json_object_new_string("strike-brick"));
    json_object_object_add(out_json,"attach",brick_object);
    update_post(out_json);
    send_message(client);
    // Recibir respuesta y actualizar
    receive_message(client);
    playerUpdateFromResponse();
    // Limpiar
    json_object_put(out_json);
}

/* */
void moveBall(int index, int status){
    Ball ball = balls[index];
    double x = (double)ball.x/(double)WINDOW_WIDTH * 100;
    double y = (double)ball.y/(double)WINDOW_HEIGHT * 100;
    struct json_object* position = json_object_new_array();
        json_object_array_add(position, json_object_new_double(x));
        json_object_array_add(position, json_object_new_double(y));
    struct json_object* ball_object = json_object_new_object();
        json_object_object_add(ball_object,"id",json_object_new_int(ball.id));
        json_object_object_add(ball_object,"position",position);
        json_object_object_add(ball_object,"speed",json_object_new_int(abs(ball.dx)));
    // Enviar la actualizacion
    struct json_object* out_json = json_object_new_object();
    json_object_object_add(out_json,"id",json_object_new_string(client->uuid));
    json_object_object_add(out_json,"name",json_object_new_string(client_name));
    json_object_object_add(out_json,"score",json_object_new_int(score));
    json_object_object_add(out_json,"request",json_object_new_string("update-game"));
    if (status == 1){
        json_object_object_add(out_json,"action",json_object_new_string("move-ball"));
    } else if (status == 0){
        json_object_object_add(out_json,"action",json_object_new_string("rm-ball"));
    }
    json_object_object_add(out_json,"attach",ball_object);
    update_post(out_json);
    send_message(client);
    // Recibir una respuesat
    receive_message(client);
    playerUpdateFromResponse();
    // Limpiar
    json_object_put(out_json);
}

/* */
void movePlatform(){
    struct json_object* out_json = json_object_new_object();
    json_object_object_add(out_json,"id",json_object_new_string(client->uuid));
    json_object_object_add(out_json,"name",json_object_new_string(client_name));
    json_object_object_add(out_json,"score",json_object_new_int(score));
    json_object_object_add(out_json,"request",json_object_new_string("update-game"));
    json_object_object_add(out_json, "action", json_object_new_string("move-racket"));
    // Parsear el objeto
    double x = (double)platform_x_coord/(double)WINDOW_WIDTH * 100;
    struct json_object* platform_object = json_object_new_object();
        json_object_object_add(platform_object, "position", json_object_new_double(x));
        json_object_object_add(platform_object, "size", json_object_new_int(platform_width/PLATFORM_WIDTH));
    // Enviar el mensaje de actualizacion
    json_object_object_add(out_json, "attach", platform_object);
    update_post(out_json);
    send_message(client);
    // Recibir respuesta
    receive_message(client);
    playerUpdateFromResponse();
    // Limpiar
    json_object_put(out_json);
}

/* */
void applyBrickPowerUp(int tipo, int index) {
    Brick bloque = bricks[index];
    int necesario = 1;
    // Activar el powerup guardado en el ladrillo
    struct json_object* out_json = json_object_new_object();
    json_object_object_add(out_json,"id",json_object_new_string(client->uuid));
    json_object_object_add(out_json,"name",json_object_new_string(client_name));
    json_object_object_add(out_json,"score",json_object_new_int(score));
    json_object_object_add(out_json,"request",json_object_new_string("update-game"));
    switch (tipo) {
        case 0:
            necesario = 0;
            break;
        case 1: 
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:add-life"));
            life++;
            json_object_object_add(out_json,"attach",json_object_new_int(life));
            update_post(out_json);
            send_message(client);
            break;
        case 2:
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:add-ball"));
            for (int i = 0; i < BALL_LIMIT; i++) {
                if (!balls[i].activa) {
                    srand(time(NULL));
                    // Configurar propiedades
                    balls[i].activa = TRUE;
                    balls[i].x = platform_x_coord + platform_width / 2;
                    balls[i].y = WINDOW_HEIGHT - PLATFORM_HEIGHT - BALL_SIZE - 20;
                    balls[i].dx = DEFAULT_BALL_SPEED;
                    balls[i].dy = -DEFAULT_BALL_SPEED;
                    balls[i].id = rand() % 10000;
                    // Parsear una nueva bola
                    double x = balls[i].x / WINDOW_WIDTH * 100;
                    double y = balls[i].y / WINDOW_HEIGHT * 100;
                    struct json_object* posicion = json_object_new_array();
                        json_object_array_add(posicion,json_object_new_int((int)x));
                        json_object_array_add(posicion,json_object_new_int((int)y));
                    struct json_object* bola = json_object_new_object();
                        json_object_object_add(bola,"id",json_object_new_int(balls[i].id));
                        json_object_object_add(bola,"position",posicion);
                        json_object_object_add(bola,"speed",json_object_new_int(abs(balls[i].dx)/DEFAULT_BALL_SPEED));
                    // Enviar la actualizacion
                    json_object_object_add(out_json,"attach",bola);
                    update_post(out_json);
                    send_message(client);
                    break;
                }
            }
            break;
        case 3: 
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:increase-racket-size"));
            platform_width *= 2; 
            json_object_object_add(out_json,"attach",json_object_new_int(platform_width/PLATFORM_WIDTH));
            update_post(out_json);
            send_message(client);
            break;
        case 4:
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:increase-racket-size"));
            platform_width /= 2; 
            if (platform_width < PLATFORM_WIDTH / 2) platform_width = PLATFORM_WIDTH / 2; 
            json_object_object_add(out_json,"attach",json_object_new_int(platform_width/PLATFORM_WIDTH));
            update_post(out_json);
            send_message(client);
            break;
        case 5: 
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:increase-ball-speed"));
            ball_speed += 2; 
            json_object_object_add(out_json,"attach",json_object_new_int(ball_speed));
            update_post(out_json);
            send_message(client);
            break;
        case 6: 
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:increase-ball-speed"));
            if (ball_speed > 2) ball_speed -= 2;
            json_object_object_add(out_json,"attach",json_object_new_int(ball_speed));
            update_post(out_json);
            send_message(client);
            break;
    }
    if (necesario == 1){
        receive_message(client);
        playerUpdateFromResponse();
    }
    json_object_put(out_json);
}

gboolean playerTimeout(gpointer data) {
    // Siempre manda un mensaje intermitente de que el juego no ha actualizado nada
    struct json_object* out_json = json_object_new_object();
    json_object_object_add(out_json,"id",json_object_new_string(client->uuid));
    json_object_object_add(out_json,"name",json_object_new_string(client_name));
    json_object_object_add(out_json,"request",json_object_new_string("no-update"));
    json_object_object_add(out_json,"score",json_object_new_int(score));
    update_post(out_json);
    send_message(client);
    json_object_put(out_json);
        out_json = NULL;

    // Se recibe una respuesta que puede o no modificar el estado del juego
    receive_message(client);
    playerUpdateFromResponse();
    // Logica normal del juego
        for (int i = 0; i < BALL_LIMIT; i++) {
            if (!balls[i].activa) continue;
            // Movimiento
            balls[i].x += balls[i].dx;
            balls[i].y += balls[i].dy;

            moveBall(i, 1);

            // Rebote en los bordes
            if (balls[i].x <= BALL_SIZE || balls[i].x >= WINDOW_WIDTH - BALL_SIZE) {
                balls[i].dx = -balls[i].dx;
            }
            if (balls[i].y <= BALL_SIZE) {
                balls[i].dy = -balls[i].dy;
            }
            // Rebote  plat
            if (balls[i].y >= WINDOW_HEIGHT - PLATFORM_HEIGHT - 10 - BALL_SIZE &&
                balls[i].x >= platform_x_coord && balls[i].x <= platform_x_coord + platform_width) {
                balls[i].dy = -balls[i].dy;
            }

            // Colisiónbloques
            for (int j = 0; j < MAX_COLUMNS*MAX_ROWS; j++) {
                if (bricks[j].visible &&
                    balls[i].x >= bricks[j].x &&
                    balls[i].x <= bricks[j].x + bricks[j].ancho &&
                    balls[i].y >= bricks[j].y &&
                    balls[i].y <= bricks[j].y + bricks[j].alto) {
                    // Modificacion en logica de juego local
                    bricks[j].visible = FALSE;
                    score += 10;
                    // Envio de mensaje al servidor
                    brickCollision(j);
                    applyBrickPowerUp(bricks[j].tipo, j);
                    balls[i].dy = -balls[i].dy;
                    break;
                }
            }

            // límite inferior, se pierde vida
            if (balls[i].y > WINDOW_HEIGHT) {
                balls[i].activa = FALSE;
                active_balls--;
                life--;
                moveBall(i,0);
                if (life <= 0) {
                    g_print("Juego terminado\n");
                        out_json = json_object_new_object();
                        json_object_object_add(out_json,"id",json_object_new_string(client->uuid));
                        json_object_object_add(out_json,"name",json_object_new_string(client_name));
                        json_object_object_add(out_json, "request", json_object_new_string("end-connection"));
                        update_post(out_json);
                        send_message(client);
                        json_object_put(out_json);
                        close(client->sock_container.socket);
                    free(client);
                    gtk_main_quit();
                } else {
                    // Reiniciar posición de la bola principal si quedan vidas
                    balls[0].activa = TRUE;
                    balls[0].x = platform_x_coord + platform_width / 2;
                    balls[0].y = WINDOW_HEIGHT - PLATFORM_HEIGHT - BALL_SIZE - 20;
                    balls[0].dx = DEFAULT_BALL_SPEED;
                    balls[0].dy = -DEFAULT_BALL_SPEED;
                    active_balls = 1;  // Resetea el contador de bolas activas
                }
            }
        }
    gtk_widget_queue_draw(DrawArea);
    return TRUE;
}


gboolean onKeyPressPlayer(GtkWidget *widget, GdkEventKey *event, gpointer data) {
    if (event->keyval == GDK_KEY_Left) {
        platform_x_coord -= 20;
        if (platform_x_coord < 0) platform_x_coord = 0;
        movePlatform();
    }
    if (event->keyval == GDK_KEY_Right) {
        platform_x_coord += 20;
        if (platform_x_coord > WINDOW_WIDTH - platform_width) platform_x_coord = WINDOW_WIDTH - platform_width;
        movePlatform();
    }
    return TRUE;
}

// ---------------------------------------------------------------------------------------------------------------------------------------
// Funciones del ESPECATDOR
// ---------------------------------------------------------------------------------------------------------------------------------------

/* */
void nullInitiate(){
    int counter = 0;
    for (int i = MAX_ROWS-1; i >= 0; i--) {
        for (int j = MAX_COLUMNS-1; j >= 0; j--){
            bricks[counter].x = (counter % 8) * (BRICK_WIDTH + 5) + 10;
            bricks[counter].y = (counter / 8) * (BRICK_HEIGHT + 5) + 10;
            bricks[counter].ancho = BRICK_WIDTH;
            bricks[counter].alto = BRICK_HEIGHT;
            bricks[counter].visible = TRUE;
            bricks[counter].tipo = 0;
            bricks[counter].num_color = 0;
            bricks[counter].mod = 0;
            counter++;
        }
    }

    // Inicializar bola inicial
    balls[0].x = (WINDOW_WIDTH-BALL_SIZE)/2;
    balls[0].y = (WINDOW_HEIGHT-BALL_SIZE)/2;
    balls[0].dx = DEFAULT_BALL_SPEED;
    balls[0].dy = DEFAULT_BALL_SPEED;
    balls[0].activa = TRUE;
    balls[0].id = 0;

    platform_x_coord = (WINDOW_WIDTH - PLATFORM_WIDTH) / 2;
    platform_width = PLATFORM_WIDTH;
}

const char* formSpectatorRequest(int type){
    switch (type){
        case 1:
            return "next-player";
        case 2:
            return "prev-player";
        case 3:
            return "spectating-player";
        case 4:
            return "end-connection";
        case 5:
            return "spectator-lost-visual";
    }
    return "waiting-for-player";
}

/* */
void reconfigureFromUpdate(struct json_object* contents){
    // Inicializar bloques
    int counter = 0;
    struct json_object* bricks_list = json_object_object_get(contents,"bricks");
    for (int i = MAX_ROWS-1; i >= 0; i--) {
        struct json_object* layer = json_object_array_get_idx(bricks_list,i);
        for (int j = MAX_COLUMNS-1; j >= 0; j--){
            struct json_object* brick = json_object_array_get_idx(layer,j);
            struct json_object* power = json_object_object_get(brick,"powerup");
            printf("Brick: %s\n",json_object_get_string(brick));
            bricks[counter].x = (counter % 8) * (BRICK_WIDTH + 5) + 10;
            bricks[counter].y = (counter / 8) * (BRICK_HEIGHT + 5) + 10;
            bricks[counter].ancho = BRICK_WIDTH;
            bricks[counter].alto = BRICK_HEIGHT;
            if (json_object_get_int(json_object_object_get(brick,"durability"))==0){
                bricks[counter].visible = FALSE;
            } else {
                bricks[counter].visible = TRUE;
            }
            bricks[counter].tipo = determinePower(power);
            bricks[counter].num_color = determineColor(json_object_get_string(json_object_object_get(brick,"color")));
            if (strcmp(json_object_get_string(power),"none")!=0){
                bricks[counter].mod = json_object_get_int(json_object_object_get(power,"value"));
            } else {
                bricks[counter].mod = 0;
            }
            counter++;
        }
    }
    // Reconfigurar todas las bolas
    struct json_object* balls_list = json_object_object_get(contents,"balls");
    int balls_list_size = json_object_array_length(balls_list);
    for (int i = 0; i < BALL_LIMIT; i++){
        if (i < balls_list_size){
            struct json_object* ball = json_object_array_get_idx(balls_list,i);
            struct json_object* position = json_object_object_get(ball,"position");
            printf("Ball: %s\n",json_object_get_string(ball));

            double x = (json_object_get_double(json_object_array_get_idx(position,0))/100)*WINDOW_WIDTH;
            double y = (json_object_get_double(json_object_array_get_idx(position,1))/100)*WINDOW_HEIGHT;
            int speed_mult = json_object_get_int(json_object_object_get(ball,"speed"));

            balls[i].x = (int)x;
            balls[i].y = (int)y;
            balls[i].dx = DEFAULT_BALL_SPEED*speed_mult;
            balls[i].dy = DEFAULT_BALL_SPEED*speed_mult,
            balls[i].activa = TRUE;
            balls[i].id = json_object_get_int(json_object_object_get(ball,"id"));
        } else {
            balls[i].x = 0;
            balls[i].y = 0;
            balls[i].dx = 0;
            balls[i].dy = 0,
            balls[i].activa = FALSE;
            balls[i].id = 0;
        }
    }
    // Inicializar plataforma
    struct json_object* platform = json_object_object_get(contents,"racket");
    printf("Racket: %s\n",json_object_get_string(platform));

    double x = (json_object_get_double(json_object_object_get(platform,"position"))/100)*WINDOW_WIDTH;
    platform_x_coord = (int)x;
    platform_width = PLATFORM_WIDTH*json_object_get_int(json_object_object_get(platform,"size"));
}

void spectatorUpdateFromResponse(){
    struct json_object* in_json = json_tokener_parse(client->INbuffer);
    const char* response = json_object_get_string(json_object_object_get(in_json,"response"));
    if (strcmp(response,"no-player-yet")==0){
        spectator_selection = 0;
    } else if (strcmp(response,"session-acquired")==0){
        spectator_selection = 3;
        reconfigureFromUpdate(json_object_object_get(in_json, "attach"));
    } else if (strcmp(response,"session-data-received")==0){
        spectator_selection = 3;
        struct json_object* changes = json_object_object_get(in_json,"attach");
        const char* imposed_request = json_object_get_string(json_object_object_get(changes,"request"));
        if (strcmp(imposed_request,"update-game")==0){
            const char* imposed_change = json_object_get_string(json_object_object_get(changes,"action"));
            if (strcmp(imposed_change,"move-ball")==0){
                struct json_object* ball_object = json_object_object_get(changes,"attach");
                struct json_object* position = json_object_object_get(ball_object,"position");
                double x = (json_object_get_double(json_object_array_get_idx(position,0))/100)*WINDOW_WIDTH;
                double y = (json_object_get_double(json_object_array_get_idx(position,1))/100)*WINDOW_HEIGHT;

                int target_ball_id = json_object_get_int(json_object_object_get(ball_object,"id"));
                for (int i = 0; i < BALL_LIMIT; i++){
                    Ball ball = balls[i];
                    if (ball.id == target_ball_id && ball.activa == TRUE){
                        ball.x = (int)x;
                        ball.y = (int)y;
                        ball.dx = DEFAULT_BALL_SPEED;
                        ball.dy = DEFAULT_BALL_SPEED;
                        break;
                    }
                }
            } else if (strcmp(imposed_change,"strike-brick")==0){
                struct json_object* brick_object = json_object_object_get(changes,"attach");
                struct json_object* position = json_object_object_get(brick_object,"position");

                int i = json_object_get_int(json_object_array_get_idx(position,0));
                int j = json_object_get_int(json_object_array_get_idx(position,1));
                int index = MAX_COLUMNS*i + j;
                bricks[index].visible = FALSE;
            } else if (strcmp(imposed_change,"add-ball")==0){
                struct json_object* ball_object = json_object_object_get(changes,"attach");
                struct json_object* position = json_object_object_get(ball_object,"position");
                double x = (json_object_get_double(json_object_array_get_idx(position,0))/100)*WINDOW_WIDTH;
                double y = (json_object_get_double(json_object_array_get_idx(position,1))/100)*WINDOW_HEIGHT;

                int new_ball_id = json_object_get_int(json_object_object_get(ball_object,"id"));
                for (int i = 0; i < BALL_LIMIT; i++){
                    Ball ball = balls[i];
                    if (ball.activa == FALSE){
                        ball.x = (int)x;
                        ball.y = (int)y;
                        ball.dx = DEFAULT_BALL_SPEED;
                        ball.dy = DEFAULT_BALL_SPEED,
                        ball.activa = TRUE;
                        ball.id = new_ball_id;
                        break;
                    }
                }
            } else if (strcmp(imposed_change,"rm-ball")==0){
                struct json_object* ball_object = json_object_object_get(changes,"attach");
                int target_ball_id = json_object_get_int(json_object_object_get(ball_object,"id"));
                for (int i = 0; i < BALL_LIMIT; i++){
                    Ball ball = balls[i];
                    if (ball.id == target_ball_id){
                        ball.x = 0;
                        ball.y = 0;
                        ball.dx = 0;
                        ball.dy = 0,
                        ball.activa = FALSE;
                        ball.id = 0;
                        break;
                    }
                }
            } else if (strcmp(imposed_change,"move-racket")==0){
                struct json_object* platform_object = json_object_object_get(changes,"attach");
                double x_position = (json_object_get_double(json_object_object_get(platform_object,"position"))/100)*WINDOW_WIDTH;
                int size_mult = json_object_get_int(json_object_object_get(platform_object,"speed"));

                platform_x_coord = (int)x_position;
                platform_width = PLATFORM_WIDTH*size_mult;
            }
        } else if (strcmp(imposed_request,"end-connection")==0){
            spectator_selection = 5;
        }
    } else if (strcmp(response,"closing")==0){
        online = 0;
    } else if (strcmp(response,"no-update")==0){
        spectator_selection = 0;
    }
    json_object_put(in_json);
    clear_tray();
}

/* */
gboolean spectactorTimeout(gpointer data){
    struct json_object* out_json = json_object_new_object();
    json_object_object_add(out_json,"id",json_object_new_string(client->uuid));
    json_object_object_add(out_json,"name",json_object_new_string(client_name));
    json_object_object_add(out_json,"request",json_object_new_string(formSpectatorRequest(spectator_selection)));

    update_post(out_json);
    send_message(client);

    receive_message(client);
    spectatorUpdateFromResponse();

    if (online == 0){
        gtk_main_quit();
    }
    return TRUE;
}

/* */
gboolean onKeyPressSpectator(GtkWidget *widget, GdkEventKey *event, gpointer data) {
    if (event->keyval == GDK_KEY_Left) {
        spectator_selection = 2;
    }
    if (event->keyval == GDK_KEY_Right) {
        spectator_selection = 1;
    }
    return TRUE;
}

// ---------------------------------------------------------------------------------------------------------------------------------------
// Metodos de inicio y ejecucion del programa
// ---------------------------------------------------------------------------------------------------------------------------------------

/* */
void openGameEvent(GtkWidget *widget, gpointer data) {
    Bundle* widgets = (Bundle *) data;
    const char *button_label = gtk_button_get_label(GTK_BUTTON(widget));
    const char *username_text = gtk_entry_get_text(GTK_ENTRY(widgets->widget1));
        client_name = malloc(sizeof(username_text));
        strcpy(client_name,username_text);
    const char *ip_text = gtk_entry_get_text(GTK_ENTRY(widgets->widget2));
        ip = malloc(sizeof(ip_text));
        strcpy(ip,ip_text);
    const char *port_text = gtk_entry_get_text(GTK_ENTRY(widgets->widget3));
        port = atoi(port_text);
    if (strcmp(button_label,"Jugador")==0){
        ct = PLAYER;
    } else if (strcmp(button_label,"Espectador")==0){
        ct = SPECTATOR;
    }
    g_print("Botón presionado: %s\n", button_label);
    g_print("Nombre de usuario: %s\n", client_name);
    g_print("IP: %s\n", ip);
    g_print("PUERTO: %d\n", port);

    GtkWidget* window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    if (ct == PLAYER){
        gtk_window_set_title(GTK_WINDOW(window), "BreakOut: Jugador");
    } else if (ct == SPECTATOR){
        gtk_window_set_title(GTK_WINDOW(window), "BreakOut: Espectador");
    }
    gtk_window_set_default_size(GTK_WINDOW(window), WINDOW_WIDTH, WINDOW_HEIGHT);
    gtk_window_set_resizable(GTK_WINDOW(window), FALSE);

    DrawArea = gtk_drawing_area_new();
    gtk_container_add(GTK_CONTAINER(window), DrawArea);
    g_signal_connect(DrawArea, "draw", G_CALLBACK(drawOnEvent), NULL);
    g_signal_connect(window, "destroy", G_CALLBACK(onExitEvent), NULL);
    if (ct == PLAYER){ //@AdrMuAl: esto no se si sirva, pero es para desactivar las teclas cuando el cliente != jugador 
        g_signal_connect(window, "key-press-event", G_CALLBACK(onKeyPressPlayer), NULL);
    } else if ( ct == SPECTATOR){
        // g_signal_connect(window, "key-press-event", G_CALLBACK(onKeyPressSpectator), NULL);
    }
    client = create_client(port, ip);
    struct json_object* out_json = json_object_new_object();
    out_json = json_object_new_object();
    // --> Configuracion inicial del mensaje de conexion
    json_object_object_add(out_json, "id", json_object_new_string(client->uuid));
    json_object_object_add(out_json, "name", json_object_new_string(client_name));
    switch (ct){
        case PLAYER:
            json_object_object_add(out_json, "type", json_object_new_string("player"));
            break;
        case SPECTATOR:
            json_object_object_add(out_json, "type", json_object_new_string("spectator"));
            break;
    }
    json_object_object_add(out_json, "request", json_object_new_string("connect"));
    // Se envia la primer peticion al servidor ()
    update_post(out_json);
    send_message(client);
    // Se recibe una respuesta del servidor
    receive_message(client);
    struct json_object* in_json = json_tokener_parse(client->INbuffer);
    const char* response = json_object_get_string(json_object_object_get(in_json,"response"));
    // El cliente y el juego deben esperar a ser aprobados para poder comenzar
    while (online == 1){
        clear_tray();
        response = json_object_get_string(json_object_object_get(in_json,"response"));
        if (strcmp(response,"approved")==0){
            if (ct == PLAYER){
                json_object_object_add(out_json, "request", json_object_new_string("init"));
            } else if (ct == SPECTATOR){
                json_object_object_add(out_json, "request", json_object_new_string("waiting-for-player"));
            }
            update_post(out_json);
            send_message(client);
        } else if (strcmp(response,"session-created")==0) {
            printf("%s!\n",response);
            struct json_object* contents = json_object_object_get(in_json,"attach");
            initiateByResponse(contents);
                json_object_put(in_json);
            break;
        } else if (strcmp(response,"session-acquired")==0) {
            printf("%s!\n",response);
            struct json_object* contents = json_object_object_get(in_json,"attach");
            initiateByResponse(contents);
                json_object_put(in_json);
            spectator_selection = 3;
            break;
        } else if (strcmp(response,"no-player-yet")==0) {
            printf("%s!\n",response);
            nullInitiate();
            spectator_selection = 0;
            break;
        } else if (strcmp(response,"standby")==0){
            if (ct == PLAYER){
                json_object_object_add(out_json, "request", json_object_new_string("on-standby"));
            } else if (ct == SPECTATOR){
                json_object_object_add(out_json, "request", json_object_new_string("waiting-for-player"));
            }
            update_post(out_json);
            send_message(client);
        } else if (strcmp(response,"rejected")==0){
            json_object_object_add(out_json, "request", json_object_new_string("end-connection"));
            update_post(out_json);
            send_message(client);
        } else if (strcmp(response,"closing")==0){
            close(client->sock_container.socket);
            free(client);
            gtk_main_quit();
            return;
        }
        json_object_put(in_json);
            in_json = NULL;
        receive_message(client);
            in_json = json_tokener_parse(client->INbuffer);
        // sleep(1);
    }
    if ( ct == PLAYER){
        g_timeout_add(30, playerTimeout, NULL);
    } else if ( ct == SPECTATOR) {
        g_timeout_add(60, spectactorTimeout, NULL);
    }
    gtk_widget_show_all(window);
    
    gtk_widget_hide(GTK_WIDGET(widgets->widget4));
    return;
}

int main(int argc, char *argv[]) {
    gtk_init(&argc, &argv);

    GtkWidget *window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(window), "Ventana de Usuario");
    gtk_window_set_default_size(GTK_WINDOW(window), 300, 250);
    gtk_container_set_border_width(GTK_CONTAINER(window), 10);

    g_signal_connect(window, "destroy", G_CALLBACK(gtk_main_quit), NULL);

    GtkWidget *vbox = gtk_box_new(GTK_ORIENTATION_VERTICAL, 5);
    gtk_container_add(GTK_CONTAINER(window), vbox);
    // ENTRADAS DE USUARIO
    GtkWidget *username_entry = gtk_entry_new();
    gtk_entry_set_placeholder_text(GTK_ENTRY(username_entry), "Username");
    gtk_box_pack_start(GTK_BOX(vbox), username_entry, TRUE, TRUE, 0);

    GtkWidget *ip_entry = gtk_entry_new();
    gtk_entry_set_placeholder_text(GTK_ENTRY(ip_entry), "IP");
    gtk_box_pack_start(GTK_BOX(vbox), ip_entry, TRUE, TRUE, 0);

    GtkWidget *port_entry = gtk_entry_new();
    gtk_entry_set_placeholder_text(GTK_ENTRY(port_entry), "Puerto");
    gtk_box_pack_start(GTK_BOX(vbox), port_entry, TRUE, TRUE, 0);
    
    Bundle package = {username_entry, ip_entry, port_entry, window, NULL};
    // BOTONES
    GtkWidget *hbox = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 5);
    gtk_box_pack_start(GTK_BOX(vbox), hbox, TRUE, TRUE, 0);

    GtkWidget *player_button = gtk_button_new_with_label("Jugador");
    g_signal_connect(player_button, "clicked", G_CALLBACK(openGameEvent), &package);
    gtk_box_pack_start(GTK_BOX(hbox), player_button, TRUE, TRUE, 0);

    GtkWidget *spectator_button = gtk_button_new_with_label("Espectador");
    g_signal_connect(spectator_button, "clicked", G_CALLBACK(openGameEvent), &package);
    gtk_box_pack_start(GTK_BOX(hbox), spectator_button, TRUE, TRUE, 0);
    gtk_widget_show_all(window);

    gtk_main();
    return 0;
}


