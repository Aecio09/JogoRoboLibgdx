package io.github.projetoalgoritmos.controller;

import io.github.projetoalgoritmos.datastructures.CustomLinkedList;
import io.github.projetoalgoritmos.model.Robo;

public class GameController {
    private CustomLinkedList<Robo> oficina;
    private int limiteRobos;
    private int totalRobosConsertados;

    public GameController(int limiteRobos) {
        this.oficina = new CustomLinkedList<>();
        this.limiteRobos = limiteRobos;
        this.totalRobosConsertados = 0;
    }

    public boolean adicionarRobo(Robo robo) {
        if (oficina.size() < limiteRobos) {
            oficina.add(robo);
            return true;
        }
        return false; // Oficina cheia
    }

    public void processarConserto(Robo robo) {
        if (robo.isConsertado()) {
            if (oficina.remove(robo)) {
                totalRobosConsertados++;
            }
        }
    }

    public CustomLinkedList<Robo> getOficina() {
        return oficina;
    }

    public int getTotalRobosConsertados() {
        return totalRobosConsertados;
    }
}
