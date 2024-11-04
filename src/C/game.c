#include <gtk/gtk.h>
#include <json-c/json.h>

#include <stdlib.h>
#include <time.h>
#include <math.h>
#include <stdio.h>
#include <string.h>

#include "modules/network/socket.h"

#define ANCHO_VENTANA 600
#define ALTO_VENTANA 600
#define ANCHO_PLATAFORMA 80
#define ALTO_PLATAFORMA 10
#define TAMANO_BOLA 10
#define ANCHO_BLOQUE 60
#define ALTO_BLOQUE 20
#define FILAS_BLOQUES 4
#define COLUMNAS_BLOQUES 8
#define NUM_BLOQUES 32
#define VELOCIDAD_INICIAL_BOLA 4
#define VIDAS_INICIALES 3
#define MAX_BOLAS 10

typedef struct {
    int x, y;
    int dx, dy;
    gboolean activa;
    int id;
} Bola;

typedef struct {
    int x, y;
    int ancho, alto;
    gboolean visible;
    int tipo; // 0 = normal, 1 = vida extra, 2 = bola extra, 3 = raqueta doble, 4 = raqueta mitad, 5 = velocidad +
    int num_color;
    int mod;
} Bloque;

enum client_type {
    PLAYER,
    SPECTATOR
};

// Atributos auxiliares para el socket
int online = 1;
enum client_type ct = PLAYER;
int port = 8080;
char* ip = "127.0.0.1";
char* client_name = "Mr.Nobody";

// Objetos para el socket
struct client* client;
struct json_object* out_json;
struct json_object* in_json;

static GtkWidget *areaDibujo;
static int plataformaX = (ANCHO_VENTANA - ANCHO_PLATAFORMA) / 2;
static int anchoPlataforma = ANCHO_PLATAFORMA;
static int vidas = VIDAS_INICIALES;
static int velocidadBola = VELOCIDAD_INICIAL_BOLA;
static int bolasActivas = 1;
static Bola bolas[MAX_BOLAS];
static Bloque bloques[FILAS_BLOQUES*COLUMNAS_BLOQUES];
static int puntaje = 0;


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

void update_post(){
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

void initiateByResponse(){
    struct json_object* contents = json_object_object_get(in_json,"attach");
    // Inicializar bloques
    int counter = 0;
    struct json_object* bricks = json_object_object_get(contents,"bricks");
    for (int i = FILAS_BLOQUES-1; i >= 0; i--) {
        struct json_object* layer = json_object_array_get_idx(bricks,i);
        printf("%s\n",json_object_get_string(layer));
        for (int j = COLUMNAS_BLOQUES-1; j >= 0; j--){
            struct json_object* brick = json_object_array_get_idx(layer,j);
            struct json_object* power = json_object_object_get(brick,"powerup");
            printf("Brick: %s\n",json_object_get_string(brick));
            bloques[counter].x = (counter % 8) * (ANCHO_BLOQUE + 5) + 10;
            bloques[counter].y = (counter / 8) * (ALTO_BLOQUE + 5) + 10;
            bloques[counter].ancho = ANCHO_BLOQUE;
            bloques[counter].alto = ALTO_BLOQUE;
            bloques[counter].visible = TRUE;
            bloques[counter].tipo = determinePower(power);
            bloques[counter].num_color = determineColor(json_object_get_string(json_object_object_get(brick,"color")));
            bloques[counter].mod = json_object_get_int(json_object_object_get(power,"value"));
            counter++;
        }
    }
    // Inicializar bolas
    struct json_object* balls = json_object_object_get(contents,"balls");
    for(int i = 0; i < json_object_array_length(balls); i++){
        struct json_object* ball = json_object_array_get_idx(balls,i);
        struct json_object* position = json_object_object_get(ball,"position");
        printf("Ball: %s\n",json_object_get_string(ball));

        double x = ((double)json_object_get_int(json_object_array_get_idx(position,0))/100)*ANCHO_VENTANA;
        double y = ((double)json_object_get_int(json_object_array_get_idx(position,1))/100)*ALTO_VENTANA;
        int speed_mult = json_object_get_int(json_object_object_get(ball,"speed"));
        
        bolas[i].x = (int)x;
        bolas[i].y = (int)y;
        bolas[i].dx = VELOCIDAD_INICIAL_BOLA*speed_mult;
        bolas[i].dy = VELOCIDAD_INICIAL_BOLA*speed_mult,
        bolas[i].activa = TRUE;
        bolas[i].id = json_object_get_int(json_object_object_get(ball,"id"));
    }
    // Inicializar plataforma
    struct json_object* platform = json_object_object_get(contents,"racket");
    struct json_object* position = json_object_object_get(platform,"position");
    printf("Racket: %s\n",json_object_get_string(platform));

    double x = ((double)json_object_get_int(json_object_array_get_idx(position,0))/100)*ANCHO_VENTANA;
    plataformaX = (int)x;
    anchoPlataforma = ANCHO_PLATAFORMA*json_object_get_int(json_object_object_get(platform,"size"));

    json_object_put(contents);
}

void updateFromResponse(){
    // in_json = json_tokener_parse(client->INbuffer);
    // const char* response = json_object_get_string(json_object_object_get(in_json,"response"));
    // json_object_put(in_json);
    // clear_tray();
}

void inicializarBloques() {
    for (int i = 0; i < NUM_BLOQUES; i++) {
        bloques[i].x = (i % 8) * (ANCHO_BLOQUE + 5) + 10;
        bloques[i].y = (i / 8) * (ALTO_BLOQUE + 5) + 10;
        bloques[i].ancho = ANCHO_BLOQUE;
        bloques[i].alto = ALTO_BLOQUE;
        bloques[i].visible = TRUE;

        // Asignar tipo basado en fila: 0=normal, 1=vida, 2=bola extra, etc. Esto hay que cambiarlo
        if (i < 8) bloques[i].tipo = 1; // Vida extra
        else if (i < 16) bloques[i].tipo = 2; // Bola extra
        else if (i < 24) bloques[i].tipo = 3; // Raqueta doble
        else bloques[i].tipo = 4; // Raqueta a la mitad
    }
}

gboolean enEventoDibujo(GtkWidget *widget, cairo_t *cr, gpointer data) {
    cairo_set_source_rgb(cr, 0.0, 0.0, 0.0);
    cairo_paint(cr);

    // Dibujar plataforma
    cairo_set_source_rgb(cr, 0.0, 1.0, 0.0);
    cairo_rectangle(cr, plataformaX, ALTO_VENTANA - ALTO_PLATAFORMA - 10, anchoPlataforma, ALTO_PLATAFORMA);
    cairo_fill(cr);

    // Dibujar las bolas activas
    for (int i = 0; i < MAX_BOLAS; i++) {
        if (bolas[i].activa) {
            cairo_set_source_rgb(cr, 1.0, 0.0, 0.0);
            cairo_arc(cr, bolas[i].x, bolas[i].y, TAMANO_BOLA, 0, 2 * G_PI);
            cairo_fill(cr);
        }
    }

    // Dibujar bloques de acuerdo a su tipo y color
    for (int i = 0; i < NUM_BLOQUES; i++) {
        if (bloques[i].visible) {
            switch (bloques[i].num_color) {
                case 0: cairo_set_source_rgb(cr, 0.0, 0.0, 1.0); break; // Normal
                case 1: cairo_set_source_rgb(cr, 0.0, 1.0, 0.0); break; // Vida extra
                case 2: cairo_set_source_rgb(cr, 1.0, 1.0, 0.0); break; // Bola extra
                case 3: cairo_set_source_rgb(cr, 1.0, 0.5, 0.0); break; // Raqueta doble
                case 4: cairo_set_source_rgb(cr, 1.0, 0.0, 0.0); break; // Raqueta mitad
                case 5: cairo_set_source_rgb(cr, 0.5, 0.0, 0.5); break; // Velocidad +
            }
            cairo_rectangle(cr, bloques[i].x, bloques[i].y, bloques[i].ancho, bloques[i].alto);
            cairo_fill(cr);
        }
    }

    return FALSE;
}

void colisionarBloque(int index){
    // Buscar bloque y definir su posicion en matriz
    int i = index/COLUMNAS_BLOQUES;
    int j = index - i*COLUMNAS_BLOQUES;
    Bloque bloque = bloques[index];
    // Parsear bloque
    struct json_object* posicion = json_object_new_array();
        json_object_array_add(posicion, json_object_new_int(i));
        json_object_array_add(posicion, json_object_new_int(j));
    struct json_object* objecto_bloque = json_object_new_object();
        json_object_object_add(objecto_bloque,"durability",json_object_new_int(1));
        json_object_object_add(objecto_bloque,"position",posicion);
        switch(bloque.tipo){
            case 1:
                json_object_object_add(objecto_bloque,"color",json_object_new_string("green"));
                break;
            case 2:
                json_object_object_add(objecto_bloque,"color",json_object_new_string("yellow"));
                break;
            case 3:
                json_object_object_add(objecto_bloque,"color",json_object_new_string("orange"));
                break;
            case 4:
                json_object_object_add(objecto_bloque,"color",json_object_new_string("red"));
                break;
        }
    // Enviar mensaje
    json_object_object_add(out_json,"request",json_object_new_string("update-game"));
    json_object_object_add(out_json,"action",json_object_new_string("strike-brick"));
    json_object_object_add(out_json,"score",json_object_new_int(puntaje));
    json_object_object_add(out_json,"attach",objecto_bloque);
    update_post();
    send_message(client);
    // Recibir respuesta y actualizar
    receive_message(client);
    updateFromResponse();
    // Limpiar
    json_object_object_del(out_json,"action");
    json_object_object_del(out_json,"score");
    json_object_object_del(out_json,"attach");
    json_object_put(posicion);
    json_object_put(objecto_bloque);
}

void moverBola(int index, int status){
    Bola bola = bolas[index];
    struct json_object* posicion = json_object_new_array();
        json_object_array_add(posicion, json_object_new_int(bola.x/ANCHO_VENTANA * 100));
        json_object_array_add(posicion, json_object_new_int(bola.y/ALTO_VENTANA * 100));
    struct json_object* objecto_bola = json_object_new_object();
        json_object_object_add(objecto_bola,"id",json_object_new_int(bola.id));
        json_object_object_add(objecto_bola,"position",posicion);
        json_object_object_add(objecto_bola,"speed",json_object_new_int(abs(bola.dx)));

    // Enviar la actualizacion
    json_object_object_add(out_json,"request",json_object_new_string("update-game"));
    if (status == 1){
        json_object_object_add(out_json,"action",json_object_new_string("move-ball"));
    } else if (status == 0){
        json_object_object_add(out_json,"action",json_object_new_string("rm-ball"));
    }
    json_object_object_add(out_json,"score",json_object_new_int(puntaje));
    json_object_object_add(out_json,"attach",objecto_bola);
    update_post();
    send_message(client);
    // Recibir una respuesat
    receive_message(client);
    updateFromResponse();
    // Limpiar
    json_object_put(posicion);
    json_object_put(objecto_bola);
}

void moverPlataforma(){
    json_object_object_add(out_json, "request", json_object_new_string("update-game"));
    json_object_object_add(out_json, "score", json_object_new_int(puntaje));
    json_object_object_add(out_json, "action", json_object_new_string("move-racket"));
    // Parsear el objeto
    struct json_object* posicion = json_object_new_array();
        json_object_array_add(posicion, json_object_new_int(plataformaX/ANCHO_VENTANA * 100));
        json_object_array_add(posicion, json_object_new_int(50));
    struct json_object* plataforma = json_object_new_object();
        json_object_object_add(plataforma, "position", posicion);
        json_object_object_add(plataforma, "size", json_object_new_int(anchoPlataforma/ANCHO_PLATAFORMA));
    // Enviar el mensaje de actualizacion
    json_object_object_add(out_json, "attach", plataforma);
    update_post();
    send_message(client);
    // Recibir respuesta
    receive_message(client);
    updateFromResponse();
    // Limpiar
    json_object_object_del(out_json,"attach");
    json_object_object_del(out_json,"action");
    json_object_put(posicion);
    json_object_put(plataforma);
}

void aplicarEfectoBloque(int tipo, int index) {
    Bloque bloque = bloques[index];
    // Activar el powerup guardado en el ladrillo
    json_object_object_add(out_json,"request",json_object_new_string("update-game"));
    switch (tipo) {
        case 1: 
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:add-life"));
            vidas++;
            json_object_object_add(out_json,"attach",json_object_new_int(vidas));
            update_post();
            send_message(client);
            break;
        case 2:
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:add-ball"));
            for (int i = 0; i < MAX_BOLAS; i++) {
                if (!bolas[i].activa) {
                    srand(time(NULL));
                    // Configurar propiedades
                    bolas[i].activa = TRUE;
                    bolas[i].x = plataformaX + anchoPlataforma / 2;
                    bolas[i].y = ALTO_VENTANA - ALTO_PLATAFORMA - TAMANO_BOLA - 20;
                    bolas[i].dx = VELOCIDAD_INICIAL_BOLA;
                    bolas[i].dy = -VELOCIDAD_INICIAL_BOLA;
                    bolas[i].id = rand() % 10000;
                    // Parsear una nueva bola
                    double x = bolas[i].x / ANCHO_VENTANA;
                    double y = bolas[i].y / ALTO_VENTANA;
                    struct json_object* posicion = json_object_new_array();
                        json_object_array_add(posicion,json_object_new_int((int)x));
                        json_object_array_add(posicion,json_object_new_int((int)y));
                    struct json_object* bola = json_object_new_object();
                        json_object_object_add(bola,"id",json_object_new_int(bolas[i].id));
                        json_object_object_add(bola,"position",posicion);
                        json_object_object_add(bola,"speed",json_object_new_int(VELOCIDAD_INICIAL_BOLA/abs(bolas[i].dx)));
                    // Enviar la actualizacion
                    json_object_object_add(out_json,"attach",bola);
                    update_post();
                    send_message(client);
                    // Limpiar
                    json_object_put(posicion);
                    json_object_put(bola);
                    break;
                }
            }
            break;
        case 3: 
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:increase-racket-size"));
            anchoPlataforma *= 2; 
            json_object_object_add(out_json,"attach",json_object_new_int(anchoPlataforma/ANCHO_PLATAFORMA));
            break;
        case 4:
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:increase-racket-size"));
            anchoPlataforma /= 2; 
            if (anchoPlataforma < ANCHO_PLATAFORMA / 2) anchoPlataforma = ANCHO_PLATAFORMA / 2; 
            json_object_object_add(out_json,"attach",json_object_new_int(anchoPlataforma/ANCHO_PLATAFORMA));
            break;
        case 5: 
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:increase-ball-speed"));
            velocidadBola += 2; 
            json_object_object_add(out_json,"attach",json_object_new_int(velocidadBola));
            break;
        case 6: 
            json_object_object_add(out_json,"action",json_object_new_string("apply-powerup:increase-ball-speed"));
            if (velocidadBola > 2) velocidadBola -= 2;
            json_object_object_add(out_json,"attach",json_object_new_int(velocidadBola));
            break;
    }
    receive_message(client);
    updateFromResponse();
    json_object_object_del(out_json,"attach");
    json_object_object_del(out_json,"action");
}

gboolean exit_cmd(GtkWidget *widget, GdkEvent *event, gpointer data){
    // json_object_object_add(out_json, "request", json_object_new_string("end-connection"));
    //     update_post();
    //     send_message(client);
    // close(client->sock_container.socket);
    // free(client);
    return FALSE;
}

gboolean enTemporizador(gpointer data) {
    // // Siempre manda un mensaje intermitente de que el juego no ha actualizado nada
    // json_object_object_add(out_json,"request",json_object_new_string("no-update"));
    // json_object_object_add(out_json,"score",json_object_new_int(puntaje));
    // update_post();
    // send_message(client);

    // // Se recibe una respuesta que puede o no modificar el estado del juego
    // receive_message(client);
    // updateFromResponse();
    // Logica normal del juego
        for (int i = 0; i < MAX_BOLAS; i++) {
            if (!bolas[i].activa) continue;
            // Movimiento
            bolas[i].x += bolas[i].dx;
            bolas[i].y += bolas[i].dy;

            //moverBola(i, 1);

            // Rebote en los bordes
            if (bolas[i].x <= TAMANO_BOLA || bolas[i].x >= ANCHO_VENTANA - TAMANO_BOLA) {
                bolas[i].dx = -bolas[i].dx;
            }
            if (bolas[i].y <= TAMANO_BOLA) {
                bolas[i].dy = -bolas[i].dy;
            }
            // Rebote  plat
            if (bolas[i].y >= ALTO_VENTANA - ALTO_PLATAFORMA - 10 - TAMANO_BOLA &&
                bolas[i].x >= plataformaX && bolas[i].x <= plataformaX + anchoPlataforma) {
                bolas[i].dy = -bolas[i].dy;
            }

            // Colisiónbloques
            for (int j = 0; j < NUM_BLOQUES; j++) {
                if (bloques[j].visible &&
                    bolas[i].x >= bloques[j].x &&
                    bolas[i].x <= bloques[j].x + bloques[j].ancho &&
                    bolas[i].y >= bloques[j].y &&
                    bolas[i].y <= bloques[j].y + bloques[j].alto) {
                    // Modificacion en logica de juego local
                    bloques[j].visible = FALSE;
                    puntaje += 10;
                    // Envio de mensaje al servidor
                    // colisionarBloque(j);
                    // aplicarEfectoBloque(bloques[j].tipo, j);
                    bolas[i].dy = -bolas[i].dy;
                    break;
                }
            }

            // límite inferior, se pierde vida
            if (bolas[i].y > ALTO_VENTANA) {
                bolas[i].activa = FALSE;
                bolasActivas--;
                vidas--;
                if (vidas <= 0) {
                    g_print("Juego terminado\n");
                    // json_object_object_add(out_json, "request", json_object_new_string("end-connection"));
                    //     update_post();
                    //     send_message(client);
                    // close(client->sock_container.socket);
                    // free(client);
                    gtk_main_quit();
                } else {
                    // Reiniciar posición de la bola principal si quedan vidas
                    bolas[0].activa = TRUE;
                    bolas[0].x = plataformaX + anchoPlataforma / 2;
                    bolas[0].y = ALTO_VENTANA - ALTO_PLATAFORMA - TAMANO_BOLA - 20;
                    bolas[0].dx = VELOCIDAD_INICIAL_BOLA;
                    bolas[0].dy = -VELOCIDAD_INICIAL_BOLA;
                    bolasActivas = 1;  // Resetea el contador de bolas activas
                }
            }
        }
    gtk_widget_queue_draw(areaDibujo);
    return TRUE;
}


gboolean enTeclaPresionada(GtkWidget *widget, GdkEventKey *event, gpointer data) {
    if (event->keyval == GDK_KEY_Left) {
        plataformaX -= 20;
        if (plataformaX < 0) plataformaX = 0;
        //moverPlataforma();
    }
    if (event->keyval == GDK_KEY_Right) {
        plataformaX += 20;
        if (plataformaX > ANCHO_VENTANA - anchoPlataforma) plataformaX = ANCHO_VENTANA - anchoPlataforma;
        //moverPlataforma();
    }
    return TRUE;
}

int main(int argc, char *argv[]) {
    gtk_init(&argc, &argv);

    GtkWidget *ventana = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title(GTK_WINDOW(ventana), "BreakOut");
    gtk_window_set_default_size(GTK_WINDOW(ventana), ANCHO_VENTANA, ALTO_VENTANA);
    gtk_window_set_resizable(GTK_WINDOW(ventana), FALSE);

    areaDibujo = gtk_drawing_area_new();
    gtk_container_add(GTK_CONTAINER(ventana), areaDibujo);
    g_signal_connect(areaDibujo, "draw", G_CALLBACK(enEventoDibujo), NULL);
    g_signal_connect(ventana, "destroy", G_CALLBACK(gtk_main_quit), NULL);
    if (ct == PLAYER){ //@AdrMuAl: esto no se si sirva, pero es para desactivar las teclas cuando el cliente != jugador 
        g_signal_connect(ventana, "key-press-event", G_CALLBACK(enTeclaPresionada), NULL);
    } else if ( ct == SPECTATOR){
        // TODO: Aqui debe haber un otro metodo detecteccion de teclas pero que solo cambie al siguiente/anterior jugador
    }

    client = create_client(port, ip);
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
    update_post();
    send_message(client);
    // Se recibe una respuesta del servidor
    receive_message(client);
    in_json = json_tokener_parse(client->INbuffer);
    const char* response = json_object_get_string(json_object_object_get(in_json,"response"));
    // El cliente y el juego deben esperar a ser aprobados para poder comenzar
    while (online == 1){
        clear_tray();
        response = json_object_get_string(json_object_object_get(in_json,"response"));
        if (strcmp(response,"approved")==0){
            json_object_object_add(out_json, "request", json_object_new_string("init"));
            update_post();
            send_message(client);
        } else if (strcmp(response,"session-created")==0) {
            json_object_object_add(out_json, "request", json_object_new_string("end-connection"));
            update_post();
            send_message(client);
            // Al enviar este mensaje el servidor una respuesta con todos los objetos en formato json
            // Estos objetos se deben extraer y crear los objetos del cliente en base a lo que diga el servidor
            break;
        }else if (strcmp(response,"standby")==0){
            json_object_object_add(out_json, "request", json_object_new_string("on-standby"));
            update_post();
            send_message(client);
        } else if (strcmp(response,"rejected")==0){
            json_object_object_add(out_json, "request", json_object_new_string("end-connection"));
            update_post();
            send_message(client);
            // La aplicacion se tiene que cerrar sola
            return 0;
        } else if (strcmp(response,"closing")==0){
            // La aplicacion se tiene que cerrar sola
            return 0;
        }
        json_object_put(in_json);
            in_json = NULL;
        receive_message(client);
            in_json = json_tokener_parse(client->INbuffer);
    }
    initiateByResponse();
    memset(client->INbuffer,0,sizeof(client->INbuffer));
    // inicializarBloques();
    //     bolas[0].x = 300;
    //     bolas[0].y = 200; 
    //     bolas[0].dx = VELOCIDAD_INICIAL_BOLA; 
    //     bolas[0].dy = VELOCIDAD_INICIAL_BOLA; 
    //     bolas[0].activa = TRUE;
    //     bolas[0].id = 64;
    g_timeout_add(20, enTemporizador, NULL);
    printf("hello\n");
    gtk_widget_show_all(ventana);
    gtk_main();
    return 0;
}


