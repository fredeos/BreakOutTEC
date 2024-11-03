#include <gtk/gtk.h>
#include <json-c/json.h>

#include <stdlib.h>
#include <time.h>
#include <stdio.h>
#include <string.h>

#include "network/socket.h"

#define ANCHO_VENTANA 600
#define ALTO_VENTANA 600
#define ANCHO_PLATAFORMA 100
#define ALTO_PLATAFORMA 10
#define TAMANO_BOLA 10
#define ANCHO_BLOQUE 60
#define ALTO_BLOQUE 20
#define NUM_BLOQUES 32
#define VELOCIDAD_INICIAL_BOLA 4
#define VIDAS_INICIALES 3
#define MAX_BOLAS 3

typedef struct {
    char* id;
    int x, y;
    int dx, dy;
    gboolean activa;
} Bola;

typedef struct {
    int x, y;
    int ancho, alto;
    gboolean visible;
    int tipo; // 0 = normal, 1 = vida extra, 2 = bola extra, 3 = raqueta doble, 4 = raqueta mitad, 5 = velocidad +
} Bloque;

typedef enum client_type {
    PLAYER,
    SPECTATOR
};

// Atributos auxiliares para el socket
int online = 1;
enum client_type ct = PLAYER;

// Objetos para el socket
struct client* client = create_client(8080,"127.0.0.1");
struct json_object* out_json = json_object_new_object();
    // --> Configuracion inicial del mensaje de conexion
    json_object_object_add(out_json, "id", json_object_new_string(client->uuid));
    json_object_object_add(out_json, "name", json_object_new_string("nameless"));
    switch (ct){
    case PLAYER:
        json_object_object_add(out_json, "type", json_object_new_string("player"));
        break;
    case SPECTATOR:
        json_object_object_add(out_json, "type", json_object_new_string("spectator"));
        break;
    }
    json_object_object_add(out_json, "request", json_object_new_string("connect"));
struct json_object* in_json;

static GtkWidget *areaDibujo;
static int plataformaX = (ANCHO_VENTANA - ANCHO_PLATAFORMA) / 2;
static int anchoPlataforma = ANCHO_PLATAFORMA;
static int vidas = VIDAS_INICIALES;
static int velocidadBola = VELOCIDAD_INICIAL_BOLA;
static int bolasActivas = 1;
static Bola bolas[MAX_BOLAS] = {
        {300, 200, VELOCIDAD_INICIAL_BOLA, VELOCIDAD_INICIAL_BOLA, TRUE}
};
static Bloque bloques[NUM_BLOQUES];
static int puntaje = 0;

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
            switch (bloques[i].tipo) {
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

void aplicarEfectoBloque(int tipo) {
    switch (tipo) {
        case 1: vidas++; break;
        case 2:
            for (int i = 0; i < MAX_BOLAS; i++) {
                if (!bolas[i].activa) {
                    bolas[i].activa = TRUE;
                    bolas[i].x = plataformaX + anchoPlataforma / 2;
                    bolas[i].y = ALTO_VENTANA - ALTO_PLATAFORMA - TAMANO_BOLA - 20;
                    bolas[i].dx = VELOCIDAD_INICIAL_BOLA;
                    bolas[i].dy = -VELOCIDAD_INICIAL_BOLA;
                    break;
                }
            }
            break;
        case 3: anchoPlataforma *= 2; break;
        case 4: anchoPlataforma /= 2; if (anchoPlataforma < ANCHO_PLATAFORMA / 2) anchoPlataforma = ANCHO_PLATAFORMA / 2; break;
        case 5: velocidadBola += 2; break;
        case 6: if (velocidadBola > 2) velocidadBola -= 2; break;
    }
}

gboolean enTemporizador(gpointer data) {
    if (online == 1){
        // Logica normal del juego
        if (ct == PLAYER){
            for (int i = 0; i < MAX_BOLAS; i++) {
                if (!bolas[i].activa) continue;

                bolas[i].x += bolas[i].dx;
                bolas[i].y += bolas[i].dy;

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
                    bloques[j].visible = FALSE;
                    puntaje += 10;
                    aplicarEfectoBloque(bloques[j].tipo);
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

            // gtk_widget_queue_draw(areaDibujo);
            // send_message(client);
            //     json_object_put(out_json);
            //     out_json = NULL:
            // receive_message(client);
            //     in_json = json_tokener_parse(client->INbuffer);
        } else if (ct == SPECTATOR){
            // Aqui va otra logica pararesponse = json_object_get_string(json_object_object_get(in,"response")); el espectador, solo que esta actualiza los objetos de juego
            // segun lo que indique el servidor
            send_message(client);
            receive_message(client);
        }
    }
    return TRUE;
}


gboolean enTeclaPresionada(GtkWidget *widget, GdkEventKey *event, gpointer data) {
    if (event->keyval == GDK_KEY_Left) {
        plataformaX -= 20;
        if (plataformaX < 0) plataformaX = 0;
    }
    if (event->keyval == GDK_KEY_Right) {
        plataformaX += 20;
        if (plataformaX > ANCHO_VENTANA - anchoPlataforma) plataformaX = ANCHO_VENTANA - anchoPlataforma;
    }
    // Parsear la estructura de la platforma a un json
    struct json_object* json_racket = json_object_new_object();

    // Enviar el mensaje de actualizacion
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

    // Se envia la primer peticion al servidor ()
    const char* str = json_object_get_string(out_json);
    size_t len = strlen(str);
    char* out_str = malloc(len+2);
    strcpy(out_str,str);
        out_str[len] = '\n';
        out_str[len+1] = '\0';
    client->OUTbuffer = out_str;
    send_message(client);
        free(out_str);
        free(client->OUTbuffer);
        json_object_put(out_json);
        out_json = NULL:

    // Se recibe una respuesta del servidor
    receive_message(client);
    in_json = json_tokener_parse(client->INbuffer);
    const char* response = json_object_get_string(json_object_object_get(in_json,"response"));
    // El cliente y el juego deben esperar a ser aprobados para poder comenzar
    while (true){
        json_object_put(in_json);
        memset(client->INbuffer,0,sizeof(client->INbuffer));

        receive_message(client);
        in_json = json_tokener_parse(client->INbuffer);
        response = json_object_get_string(json_object_object_get(in_json,"response"));
        if (strcmp(response,"approved")==0){
            out_json = json_object_new_object();
            json_object_object_add(out_json, "id", json_object_new_string(client->uuid));
            json_object_object_add(out_json, "name", json_object_new_string("nameless"));
            json_object_object_add(out_json, "request", json_object_new_string("init"));
            str = json_object_get_string(out_json);
            len = strlen(str);
            out_str = malloc(len+2);
            strcpy(out_str,str);
                out_str[len] = '\n';
                out_str[len+1] = '\0';
            client->OUTbuffer = out_str;
            send_message(client);
                free(out_str);
                free(client->OUTbuffer);
                json_object_put(out_json);
                out_json = NULL:
            // Al enviar este mensaje el servidor una respuesta con todos los objetos en formato json
            // Estos objetos se deben extraer y crear los objetos del cliente en base a lo que diga el servidor
            break;
        } else if (strcmp(response,"standby")==0){
            out_json = json_object_new_object();
            json_object_object_add(out_json, "id", json_object_new_string(client->uuid));
            json_object_object_add(out_json, "name", json_object_new_string("nameless"));
            json_object_object_add(out_json, "request", json_object_new_string("on-standby"));
            str = json_object_get_string(out_json);
            len = strlen(str);
            out_str = malloc(len+2);
            strcpy(out_str,str);
                out_str[len] = '\n';
                out_str[len+1] = '\0';
            client->OUTbuffer = out_str;
            send_message(client);
                free(out_str);
                free(client->OUTbuffer);
                json_object_put(out_json);
                out_json = NULL:
        } else if (strcmp(response,"rejected")==0 || strcmp(response,"closing")==0){
            // La aplicacion se tiene que cerrar sola
            return 0;
        }
    }

    inicializarBloques();
    g_timeout_add(20, enTemporizador, NULL);

    gtk_widget_show_all(ventana);
    gtk_main();

    return 0;
}


