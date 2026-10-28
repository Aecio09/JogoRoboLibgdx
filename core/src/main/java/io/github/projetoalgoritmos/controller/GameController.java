package io.github.projetoalgoritmos.controller;

import io.github.projetoalgoritmos.datastructures.CustomLinkedList;
import io.github.projetoalgoritmos.model.Robo;

public class GameController {
    private CustomLinkedList<Robo> oficina;
    private int limiteRobos;
    private int totalRobosConsertados;
    private int diaAtual;
    private int robosRestantesNoDia;
    private int totalDiasConcluidos;
    private float tempoRestanteDia;
    private float tempoBonusAcumulado;

    public GameController(int limiteRobos) {
        this.oficina = new CustomLinkedList<>();
        this.limiteRobos = limiteRobos;
        this.totalRobosConsertados = 0;
        this.diaAtual = 1;
        this.robosRestantesNoDia = 0;
        this.totalDiasConcluidos = 0;
        this.tempoRestanteDia = 0f;
        this.tempoBonusAcumulado = 0f;
    }

    public void iniciarDia(int dia, int totalRobos, float tempoInicial) {
        oficina.clear();
        this.diaAtual = dia;
        this.robosRestantesNoDia = totalRobos;
        this.tempoRestanteDia = tempoInicial;
        this.tempoBonusAcumulado = 0f;
    }

    public boolean adicionarRobo(Robo robo) {
        if (oficina.size() < limiteRobos) {
            oficina.add(robo);
            return true;
        }
        return false;
    }

    public void processarConserto(Robo robo) {
        if (oficina.remove(robo)) {
            totalRobosConsertados++;
            if (robosRestantesNoDia > 0) {
                robosRestantesNoDia--;
            }
        }
    }

    public void adicionarTempoBonus(float segundos) {
        tempoRestanteDia += segundos;
        tempoBonusAcumulado += segundos;
    }

    public void atualizarTempo(float delta) {
        if (tempoRestanteDia > 0f) {
            tempoRestanteDia = Math.max(0f, tempoRestanteDia - delta);
        }
    }

    public void concluirDia() {
        totalDiasConcluidos++;
    }

    public boolean isTempoEsgotado() {
        return tempoRestanteDia <= 0f;
    }

    public boolean isDiaConcluido() {
        return robosRestantesNoDia <= 0;
    }

    public CustomLinkedList<Robo> getOficina() { return oficina; }
    public int getTotalRobosConsertados() { return totalRobosConsertados; }
    public int getDiaAtual() { return diaAtual; }
    public int getRobosRestantesNoDia() { return robosRestantesNoDia; }
    public int getTotalDiasConcluidos() { return totalDiasConcluidos; }
    public float getTempoRestanteDia() { return tempoRestanteDia; }
    public float getTempoBonusAcumulado() { return tempoBonusAcumulado; }
}
