#include <gtk/gtk.h>
#include <stdlib.h>
#include <time.h>



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

    gtk_widget_queue_draw(areaDibujo);
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
    g_signal_connect(ventana, "key-press-event", G_CALLBACK(enTeclaPresionada), NULL);

    inicializarBloques();
    g_timeout_add(20, enTemporizador, NULL);

    gtk_widget_show_all(ventana);
    gtk_main();

    return 0;
}


