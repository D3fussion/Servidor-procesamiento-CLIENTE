import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
/**
 * @author Angel Laureano Borquez Fimbres (221210626)
 * @author Juan Diego Quijada Castillo (222206011)
 * @author Aaron Velez Coronado (222206601)
 */
public class Cliente_multiple {

    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("Falta la ip del servidor y el archivo que se va a leer como argumento ."); // Argumentos: localhost URLGutenberg.txt
            System.exit(1);
        }
        HashMap<String,Integer> stopwords = cargaStopWords("english_sw.txt");
        try (Socket socket = new Socket(args[0],5000)){
            System.out.println("Esperando instrucciones del servidor...");
            BufferedReader input = new BufferedReader( new InputStreamReader(socket.getInputStream())); // Con esto leo lo que me manda el servidor
            PrintWriter output = new PrintWriter(socket.getOutputStream(),true); // Con esto le mando mensajes al servidor
            String nombre = input.readLine(); // Recibo el nombre de este cliente
            System.out.println("\nNombre del cliente: "+ nombre+"\n");

            // Aqui empiezan los cambios
            int numeroDeMaquinas = Integer.parseInt(input.readLine()); // Recibo el numero de maquinas
            Scanner sc = new Scanner(System.in);
            System.out.println("¿Quieres procesar tus URLs? (y/n)");
            String respuesta = sc.nextLine();
            if(respuesta.equals("y")) {
                output.println("y");
                String s = input.readLine();
                output.println(s);

                for (int i = 0; i < numeroDeMaquinas * numeroDeMaquinas; i++) {
                    input.readLine(); // Comprueba que todas las maquinas esten listas
                }

                System.out.println();
                // Aqui
                voyASerElPrincipalMama(numeroDeMaquinas, output, args[1]);
            } else {
                output.println("n");
                String s = input.readLine();
                output.println(s);
            }
            // Aqui terminan los cambios
            String urls = input.readLine(); // Recibo las urls que le tocaron a este cliente
            System.out.println("\nUrls que te tocaron: " + urls + "\n");
            String[] urls_array = urls.split(" "); // Converti el string de urls a un array

            Instant start = Instant.now();
            descargarPaginaParelo(urls_array); // Esto descarga las paginas en paralelo

            System.out.println("\nProcesando los textos...");

            List<Future<LecturaArchivo>> lista_textos = cargaTextos("Resultados", "txt");
            String palabrasRepetidas = procesaListaTextos(lista_textos, stopwords);
            output.println(palabrasRepetidas+"$$$");

            System.out.println("Se procesaron los textos correctamente!");

            if(respuesta.equals("y")) {
                for (int i = 0; i < numeroDeMaquinas - 1; i++) {
                    input.readLine();
                }
                System.out.println("\nPalabras que se repiten en cada texto:");
                StringBuilder listaDePalabrasRepetidas = new StringBuilder();
                String linea;
                int veces=0;
                while ((linea = input.readLine()) != null) {
                    if(linea.equals("$$$")) {
                        veces++;
                    } else {
                        listaDePalabrasRepetidas.append("\n").append(linea);
                    }
                    if(veces==numeroDeMaquinas) {
                        break;
                    }
                }
                Instant end = Instant.now();
                System.out.println(listaDePalabrasRepetidas);
                System.out.println("\nTiempo de ejecucion: " + Duration.between(start, end).toMillis() + " milisegundos");

            }
        } catch (Exception e) {
            System.out.println("Main client. Error:"+e.getMessage());
        }
    }

    private static void descargarPaginaParelo(String[] urls_array){
        try {
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Future<LecturaArchivo>> processingTasks = new ArrayList<>();
            for (String url : urls_array) {
                Callable<LecturaArchivo> callable = () -> {
                    descargarPagina(url); // Descarga la pagina
                    return null;
                };
                Future<LecturaArchivo> processingTask = executor.submit(callable);
                processingTasks.add(processingTask);
            }
            for (Future<LecturaArchivo> processingTask : processingTasks) {
                if(processingTask.get() != null && !processingTask.isCancelled()) {
                    processingTask.get(); // Esto es lo que hace que lo paralelo pare hasta que tenga todos los resultados
                }
            }
            executor.shutdown();
        } catch (Exception e) {
            System.out.println("Error al descargar las paginas: " + e.getMessage());
        }
    }

    private static void descargarPagina(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            String htmlContent = document.html();
            String filePath = "Resultados\\"+document.title() + ".txt";
            try (FileWriter fileWriter = new FileWriter(filePath)) {
                fileWriter.write(htmlContent);
                System.out.println("Archivo descargado: " + document.title());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }                                 // No es un error, si son dos try catch
    }

    public static HashMap<String,Integer> cargaStopWords(String archivo) {
        HashMap<String,Integer> hash_words = new HashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(archivo));
            String linea;
            while ( (linea= reader.readLine()) !=null ) {
                hash_words.put(linea,0);
            }
        } catch (IOException e) {
            System.out.println("cargarTexto:"+e.getMessage());
        }
        return hash_words;
    }

    public static String procesaListaTextos(List<Future<LecturaArchivo>> lista, HashMap<String,Integer> stopwords) throws ExecutionException, InterruptedException {
        StringBuilder repetidos = new StringBuilder();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Void>> processingTasks = new ArrayList<>(); // Esta en ingles porque lo copie de StackOverflow
        for (Future<LecturaArchivo> textoFuture : lista) {
            Callable<Void> callable = () -> {
                LecturaArchivo texto = textoFuture.get();
                texto.cuentaPalabras();
                texto.cuentaPalabrasRepetidas();
                texto.cuentaPalabraStop(stopwords);
                repetidos.append(texto.displayPalabrasRepetidas(texto.diccionario_non_stopwords,10));
                return null;
            };

            Future<Void> processingTask = executor.submit(callable);
            processingTasks.add(processingTask);
        }

        for (Future<Void> processingTask : processingTasks) {
            processingTask.get(); // Esto es lo que hace que lo paralelo pare hasta que tenga todos los resultados
        }

        executor.shutdown();
        return repetidos.toString();
    }

    public static List<Future<LecturaArchivo>> cargaTextos(String folder, String extension){
        List<Future<LecturaArchivo>> lista = new ArrayList<>();
        List<Future<LecturaArchivo>> processingTasks = new ArrayList<>(); // Esta en ingles porque lo copie de StackOverflow
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<File> archivos = obtenListaArchivos(folder,extension);
        try {
            for (File archivo : archivos) {
                Callable<LecturaArchivo> callable = () -> {
                    LecturaArchivo txt = new LecturaArchivo(archivo.toString());
                    txt.cargarTexto();
                    return txt;
                };
                Future<LecturaArchivo> processingTask = executor.submit(callable);
                processingTasks.add(processingTask);
            }

            for (Future<LecturaArchivo> processingTask : processingTasks) {
                if(processingTask.get() != null && !processingTask.isCancelled()) {
                    lista.add(processingTask);
                }
            }

            executor.shutdown();
        } catch (Exception e) {
            System.out.println("cargaTextosSerial:"+e.getMessage());
        }
        return lista;
    }

    public static List<File> obtenListaArchivos(String ruta, String ext) {
        List<File> lista_archivos = new ArrayList<>();
        File directorio = new File(ruta);
        File[] lista = directorio.listFiles();
        for (File archivo : lista) {
            String nombre_archivo = archivo.toString();
            int indice = nombre_archivo.lastIndexOf(".");
            if (indice > 0) {
                String extension = nombre_archivo.substring(indice+1);
                if(extension.equals(ext)) {
                    lista_archivos.add(archivo);
                }
            }
        }
        return lista_archivos;
    }

    private static void voyASerElPrincipalMama(int numeroDeMaquinas, PrintWriter output, String nombreDeArchivo) {
        try {
            ArrayList<String> URLs = leerArchivo(nombreDeArchivo);
            int numeroDeUrlsPorMaquina = URLs.size() / numeroDeMaquinas;
            int numeroDeUrlsSobrantes = URLs.size() % numeroDeMaquinas;
            int j = 0;
            for(int i = 0; i < numeroDeMaquinas; i++) {
                ArrayList<String> urlsList = new ArrayList<>();
                for(int k = 0; k < numeroDeUrlsPorMaquina; k++) {
                    urlsList.add(URLs.get(j));
                    j++;
                }
                if(numeroDeUrlsSobrantes > 0) {
                    urlsList.add(URLs.get(j));
                    j++;
                    numeroDeUrlsSobrantes--;
                }
                System.out.println("URLs que le tocaron a la maquina " + (i+1) + ": " + urlsList);

                StringBuilder urls_string = new StringBuilder();
                for (String url : urlsList) {
                    urls_string.append(url).append(" ");
                }

                output.println("Maquina" + (i+1) + " " + urls_string);
            }
        } catch (Exception e) {
            System.out.println("voyASerElPrincipalMama. Error:" + e.getMessage());
        }
    }

    private static ArrayList<String> leerArchivo(String nombreDeArchivo){
        ArrayList<String> URLs = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(nombreDeArchivo));
            String linea;
            while (((linea = reader.readLine())) != null) {
                URLs.add(linea);
            }
            reader.close();
        } catch (IOException e) {
            System.out.println(e);
        }
        return URLs;
    }

}
