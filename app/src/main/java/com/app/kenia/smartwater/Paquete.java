package com.app.kenia.smartwater;

/**
 * Created by luisbarrera on 10/3/17.
 */

public class Paquete {
    public String Temperatura, Latitud, Longitud, Distancia, turbidez;

    public Paquete(String Temperatura, String Latitud, String Longitud, String Distancia, String turbidez) {
        this.Temperatura = Temperatura;
        this.Latitud = Latitud;
        this.Longitud = Longitud;
        this.Distancia = Distancia;
        this.turbidez = turbidez;
    }
}
