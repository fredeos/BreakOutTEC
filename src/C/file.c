#include <stdio.h>
#include "modules/socket.h"

int main(){
    struct client* cliente = create_client(8080, "127.0.0.1");
    cliente->OUTbuffer = "Saludos desde el cliente!";

    send_message(cliente);
    //receive_message(cliente);

    close(cliente->sock_container.socket);
    free(cliente);

    printf("Conexion finalizada suavemente!");
    return 0;
}