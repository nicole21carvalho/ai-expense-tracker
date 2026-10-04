package br.com.nicole.cofre.exception;

public class MuitasTentativasException extends RuntimeException {

    private final long segundosParaTentarDeNovo;

    public MuitasTentativasException(long segundosParaTentarDeNovo) {
        super("Muitas tentativas. Tente novamente em " + segundosParaTentarDeNovo + " segundos.");
        this.segundosParaTentarDeNovo = segundosParaTentarDeNovo;
    }

    public long getSegundosParaTentarDeNovo() {
        return segundosParaTentarDeNovo;
    }
}
