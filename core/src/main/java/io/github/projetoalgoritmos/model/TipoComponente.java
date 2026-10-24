package io.github.projetoalgoritmos.model;

public enum TipoComponente {
    MEMORIA_RAM("Memória RAM", 2.0f),
    CPU("Processador CPU", 5.0f),
    PLACA_VIDEO("Placa de Vídeo", 4.0f),
    BATERIA("Bateria", 1.5f),
    SENSOR("Sensor", 1.0f);

    private final String nome;
    private final float tempoBase;

    TipoComponente(String nome, float tempoBase) {
        this.nome = nome;
        this.tempoBase = tempoBase;
    }

    public String getNome() { return nome; }
    public float getTempoBase() { return tempoBase; }
}
