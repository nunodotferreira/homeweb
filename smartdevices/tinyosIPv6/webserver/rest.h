#ifndef REST_H
#define REST_H

char device_desc[]         = "tinyOS sensorMote Kitchen /wadl/tinyosIP6/";

char TEMPERATURE[] 	   = "/Temperature";
char HUMIDITY[] 	   = "/Humidity";
char ILLUMINATION[] 	   = "/Illumination";
char LIGHT[]    	   = "/Light";
char ALIVENESS[]	   = "/Aliveness";

char LIGHT_PARAM[] = "color";

enum {
    HTTP_GET,
    HTTP_POST,
    HTTP_PUT,
    HTTP_DELETE,
};

enum {
    S_IDLE,
    S_CONNECTED,
    S_REQUEST_PRE,
    S_REQUEST,
    S_HEADER,
    S_BODY,
};
 

#endif
