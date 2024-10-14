import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
/**
 * @author Angel Laureano Borquez Fimbres (221210626)
 * @author Juan Diego Quijada Castillo (222206011)
 * @author Aaron Velez Coronado (222206601)
 */
public class LecturaArchivo {

    String[] palabras;
    HashMap<String,Integer> diccionario_non_stopwords;
    String filename;
    String texto;
    int palabras_repetidas;
    HashMap<String, Integer> diccionario;
    ArrayList<String> Top10;

    public LecturaArchivo(String filename) {
        this.filename = filename;
        this.texto = "";
        this.palabras_repetidas = 0;
        this.Top10 = new ArrayList<>();
    }

    public void cuentaPalabraStop(HashMap<String,Integer> stopwords) {
        HashMap<String,Integer> dnostop = new HashMap<>();
        for (String palabra : this.palabras) {
            if (!stopwords.containsKey(palabra)) {
                if(dnostop.containsKey(palabra)) {
                    int conteo = dnostop.get(palabra);
                    dnostop.put(palabra,conteo+1);
                } else {
                    dnostop.put(palabra,1);
                }
            }
        }
        this.diccionario_non_stopwords = dnostop;
    }

    public void cargarTexto() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.filename));
            String linea;
            StringBuilder texto = new StringBuilder();
            while ( (linea= reader.readLine()) !=null ) {
                texto.append(linea);
            }
            this.texto = texto.toString();
        } catch (IOException e) {
            System.out.println("cargarTexto:"+e.getMessage());
        }
    }

    public void cuentaPalabras() {
        this.texto = this.texto.toLowerCase();
        this.palabras = this.texto.split("[^a-zA-Z]+");
    }

    public void cuentaPalabrasRepetidas() {
        HashMap<String,Integer> diccionario = new HashMap<>();
        for (String palabra : palabras) {
            if(diccionario.containsKey(palabra)) {
                int conteo = diccionario.get(palabra);
                diccionario.put(palabra,conteo+1);
            } else {
                diccionario.put(palabra,1);
            }
        }
        this.palabras_repetidas = diccionario.size();
        this.diccionario = diccionario;
    }

    public String displayPalabrasRepetidas(HashMap<String,Integer> diccionario,int num_pal) {
        LinkedHashMap<String,Integer> sort_dicc = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();

        ArrayList<Integer> lista = new ArrayList<>();
        for(Map.Entry<String,Integer> registro: diccionario.entrySet()) {
            lista.add(registro.getValue());
        }
        lista.sort(Collections.reverseOrder());
        for(int num : lista) {
            for(Map.Entry<String, Integer> registro : this.diccionario.entrySet() ) {
                if (registro.getValue().equals(num)) {
                    sort_dicc.put(registro.getKey(),num);
                }
            }
        }
        String filename = this.filename.substring(this.filename.lastIndexOf("\\")+1);
//        System.out.println(">>>>>>>"+this.filename);
        sb.append("------> ").append(filename).append("\n");

        int cuenta_pal =0;
        ArrayList<String> palabras =
                new ArrayList<>(sort_dicc.keySet());
        ArrayList<Integer> conteo =
                new ArrayList<>(sort_dicc.values());

        for (int i = 0; i < palabras.size(); i++) {
            if (cuenta_pal<num_pal) {
                if(conteo.get(i)>1) {
                    String palabra = palabras.get(i)+ ":" +
                            conteo.get(i);
//                    System.out.println("   " + palabra);
                    sb.append("   ").append(palabra).append("\n");
                    cuenta_pal++;
                    this.Top10.add(palabra);
                }
            }
        }
        return sb.toString();
    }
}
