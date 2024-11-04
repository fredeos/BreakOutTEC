#include <gtk/gtk.h>
//ejemplo para que sepa como usar los botones
void on_button_clicked(GtkWidget *widget, gpointer data) {
    const char *button_label = gtk_button_get_label(GTK_BUTTON(widget));
    g_print("Botón presionado: %s\n", button_label);
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
    GtkWidget *username_entry = gtk_entry_new();
    gtk_entry_set_placeholder_text(GTK_ENTRY(username_entry), "Username");
    gtk_box_pack_start(GTK_BOX(vbox), username_entry, TRUE, TRUE, 0);
    GtkWidget *ip_entry = gtk_entry_new();
    gtk_entry_set_placeholder_text(GTK_ENTRY(ip_entry), "IP");
    gtk_box_pack_start(GTK_BOX(vbox), ip_entry, TRUE, TRUE, 0);
    GtkWidget *port_entry = gtk_entry_new();
    gtk_entry_set_placeholder_text(GTK_ENTRY(port_entry), "Puerto");
    gtk_box_pack_start(GTK_BOX(vbox), port_entry, TRUE, TRUE, 0);

    // BOTONES
    GtkWidget *hbox = gtk_box_new(GTK_ORIENTATION_HORIZONTAL, 5);
    gtk_box_pack_start(GTK_BOX(vbox), hbox, TRUE, TRUE, 0);
    GtkWidget *player_button = gtk_button_new_with_label("Jugador");
    g_signal_connect(player_button, "clicked", G_CALLBACK(on_button_clicked), NULL);
    gtk_box_pack_start(GTK_BOX(hbox), player_button, TRUE, TRUE, 0);
    GtkWidget *spectator_button = gtk_button_new_with_label("Espectador");
    g_signal_connect(spectator_button, "clicked", G_CALLBACK(on_button_clicked), NULL);
    gtk_box_pack_start(GTK_BOX(hbox), spectator_button, TRUE, TRUE, 0);
    gtk_widget_show_all(window);
    gtk_main();

    return 0;
}
