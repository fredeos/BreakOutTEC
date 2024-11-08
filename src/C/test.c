#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include <json-c/json.h>
#include "modules/network/socket.h"


int main(){
    struct client* cliente = create_client(8080, "127.0.0.1");
    struct json_object* out;
    struct json_object* in;

    int online = 1;
    int startup = 1;
    while (online == 1){
        out = json_object_new_object();
        json_object_object_add(out, "id", json_object_new_string(cliente->uuid));
        json_object_object_add(out, "name", json_object_new_string("pepe"));
        json_object_object_add(out, "type", json_object_new_string("player"));
        if (startup == 0){
            receive_message(cliente);
            in = json_tokener_parse(cliente->INbuffer);
            const char* response = json_object_get_string(json_object_object_get(in,"response"));
            int code = json_object_get_int(json_object_object_get(in,"code"));
            if (code >= 100 && code < 200){
                if (strcmp(response,"standby")==0){
                    json_object_object_add(out,"request",json_object_new_string("on-standby"));
                }
                if (strcmp(response,"approved")==0){
                    json_object_object_add(out,"request",json_object_new_string("init"));
                }
                if (strcmp(response,"session-created")==0){
                    json_object_object_add(out,"request",json_object_new_string("end-connection"));
                } 
                if (strcmp(response,"rejected")==0){
                    json_object_object_add(out,"request",json_object_new_string("end-connection"));
                }
                if (strcmp(response,"closing")==0){
                    printf("cerrando el orto\n");
                    break;
                }
            } else if (code >= 400){
                json_object_object_add(out,"request",json_object_new_string("end-connection"));
            }
            json_object_put(in);
            memset(cliente->INbuffer,0,sizeof(cliente->INbuffer));
        }
        if (startup == 1){
            json_object_object_add(out, "request", json_object_new_string("connect"));
            startup = 0;
        }
        const char* j_out = json_object_get_string(out);
        size_t len = strlen(j_out);
        char* out_content = malloc(len+2);
        strcpy(out_content, j_out);
        out_content[len] = '\n';
        out_content[len+1] = '\0';
        cliente->OUTbuffer = out_content;

        send_message(cliente);
            free(cliente->OUTbuffer);
        json_object_put(out);
    }
    
    close(cliente->sock_container.socket);
    free(cliente);
    printf("Conexion finalizada suavemente!\n");
    return 0;
}