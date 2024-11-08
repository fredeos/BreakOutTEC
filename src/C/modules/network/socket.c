#include "socket.h"

int initialize(struct container *cont, int PORT, char *IP){
    // Inicializar el socket
    cont->socket = socket(AF_INET, SOCK_STREAM, 0);
    if (cont->socket < 0){
        printf("ERROR: Hubo un problema durante la creacion del socketTCP\n");
        return 0;
    }
    // Configurar la dirrecion IP indicada
    cont->address.sin_family = AF_INET;
    cont->address.sin_port = htons(PORT);
    printf("Puerto:%d\n",PORT);
    cont->address.sin_addr.s_addr = inet_addr(IP);
    printf("IP:%s\n", IP);

    return 1;
}

int start_connect(struct container *cont){
    // Intentar conectarse a la direccion almacenada en el contenedor del socket
    if (connect(cont->socket,(struct sockaddr*)&cont->address, sizeof(cont->address)) < 0){
        printf("ERROR: Conexion al servidor fallida\n");
        return 0;
    }
    return 1;
}

struct client *create_client(int port, char *server_ip){
    // Crear un espacio en memoria para un nuevo cliente
    struct client* instance = (struct client*) malloc(1*sizeof(struct client));
    if (instance == NULL){ // Error durante la asignacion de memoria
        printf("ERROR: No se pudo asignar memoria para un nuevo cliente\n");
        return NULL;
    }
    uuid_t UUID;
    uuid_unparse(UUID,instance->uuid);
    initialize(&instance->sock_container, port, server_ip);
    start_connect(&instance->sock_container);
    return instance;
}

void send_message(struct client *client){
    if (send(client->sock_container.socket, client->OUTbuffer, strlen(client->OUTbuffer), 0) < 0){
        printf("ERROR: No se pudo enviar el mensaje\n");
        return;
    }
    printf("Mensaje enviado!\n");
    return;
}

void receive_message(struct client *client){
    if (recv(client->sock_container.socket, client->INbuffer, sizeof(client->INbuffer), 0) < 0){
        printf("ERROR: No se pudo recibir respuesta del servidor\n");
        close(client->sock_container.socket);
        return;
    }
    printf("Servidor: %s\n",client->INbuffer);
    return;
}
