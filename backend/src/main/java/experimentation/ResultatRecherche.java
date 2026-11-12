package experimentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// garde les lignes trouvees avec leur numero, et compte les lignes et les caracteres lus
public class ResultatRecherche {

    public static class LigneTrouvee {

        private final long numero;
        private final String texte;

        public LigneTrouvee(long numero, String texte) {
            this.numero = numero;
            this.texte = texte;
        }

        public long getNumero() {
            return numero;
        }

        public String getTexte() {
            return texte;
        }
    }

    private final List<LigneTrouvee> lignes = new ArrayList<>();
    private long nbLignes = 0;
    private long nbCaracteres = 0;

    public ResultatRecherche() {
    }

    void ajouterLigneLue(String ligne, boolean trouvee) {
        nbLignes++;
        nbCaracteres += ligne.length();

        if (trouvee) {
            lignes.add(new LigneTrouvee(nbLignes, ligne));
        }
    }

    public List<LigneTrouvee> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    public long getNombreTrouvees() {
        return lignes.size();
    }

    public long getLignesLues() {
        return nbLignes;
    }

    public long getCaracteresLus() {
        return nbCaracteres;
    }
}
