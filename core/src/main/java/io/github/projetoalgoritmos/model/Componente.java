package io.github.projetoalgoritmos.model;

public class Componente {
    private TipoComponente tipo;

    public Componente(TipoComponente tipo) {
        this.tipo = tipo;
    }

    public TipoComponente getTipo() { return tipo; }
}
