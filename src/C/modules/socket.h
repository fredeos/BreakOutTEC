#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>        // For close()
#include <arpa/inet.h>     // For inet_addr()
#include <sys/socket.h>    // For socket functions

/// @brief A
struct container{
    int socket;
    struct sockaddr_in address;
};

/// @brief 
struct client{
    struct container sock_container;
    char INbuffer[1024];
    char *OUTbuffer;
};

/// @brief Inicializa un socket TCP dentro de un contenedor
/// @param cont Contenedor (estructura) donde se almacenan los componentes de un socket
/// @param PORT Numero de puerto a donde se configura el socket
/// @param IP IP a la configurar el socket
/// @return 1 si la inicializacion fue exitosa o 0 si hubo algun error de creacion
int initialize(struct container *cont, int PORT, char *IP);

/// @brief Intenta establecer una conexion al servidor con el puerto e IP asignados al socket.
/// @param cont Contenedor del socket y sus componentes
/// @return 1 si la conexion fue exitosa o 0 si hubo un error de conexion
int start_connect(struct container *cont);

/// @brief Crea una instance de un cliente y lo intenta conectar al servidor
struct client* create_client(int port, char *server_Ip);

/// @brief Envia un mensaje desde el cliente
/// @param client Instancia de un cliente inicializado
void send_message(struct client* client);

/// @brief Pone en espera al socket para recibir un mensaje
/// @param client Instancia del cliente que debe esperar una respuesta
void receive_message(struct client* client);


/// @brief 
/// @param cont     char buffer[1024] = {};
/// @return 
int interrupt(struct container* cont);

/// @brief 
/// @param Client 
void cancel_connection(struct client *Client);