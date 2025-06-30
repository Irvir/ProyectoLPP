import java.util.*;

// Definición de la interfaz Operaciones que describe las operaciones que debe implementar el programa.
interface Operaciones {
    void tokenizador(String input);
    int parser();
    void convertirPrefija();
    void convertirPostfija();
}

// Clase que implementa la interfaz Operaciones para manipular expresiones matemáticas.
class Programa implements Operaciones {
    // Mapa para almacenar asignaciones de variables (clave: nombre de variable, valor: su valor entero).
    private Map<String, Integer> variables = new HashMap<>();
    // Lista que contendrá los tokens de la expresión en notación prefija.
    private List<String> prefija = new ArrayList<>();
    private List<String> postfija = new ArrayList<>();
    // Nombre de la variable a la que se asignará el resultado (si hay “=” en la entrada).
    private String variableAsignacion;
    // Cadena con la parte de la expresión después del “=” (o toda la entrada si no hay asignación).
    private String expresion;
    // Lista con los tokens ya formateados de la expresión infija original.
    private List<String> expresionFormateada;



    @Override
    public void tokenizador(String input) {
        // Divide la línea en nombre de variable y expresión, si existe un “=”.
        String[] parts = input.split("=", 2);
        if (parts.length == 2) {
            variableAsignacion = parts[0].trim();     // Lado izquierdo antes del “=”
            expresion = parts[1].trim();             // Lado derecho tras el “=”
        } else {
            variableAsignacion = null;               // No hay asignación
            expresion = input.trim();                // Toda la línea es expresión
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder actual = new StringBuilder();

        // Recorre carácter a carácter la expresión para separar tokens
        for (int i = 0; i < expresion.length(); i++) {
            char c = expresion.charAt(i);
            if (Character.isWhitespace(c)) {
                continue; // Ignora espacios
            }
            if (Character.isLetterOrDigit(c)) {
                // Si es letra o dígito, acumula en el buffer actual
                actual.append(c);
            } else {
                // Si encuentra un operador o paréntesis, primero agrega el token acumulado
                if (actual.length() > 0) {
                    tokens.add(actual.toString());
                    actual.setLength(0);
                }
                // Luego agrega el carácter actual como token individual
                tokens.add(Character.toString(c));
            }
        }
        // Al finalizar, si hay algo en el buffer, se agrega
        if (actual.length() > 0) {
            tokens.add(actual.toString());
        }

        // Guarda la lista de tokens para usarse en las conversiones
        expresionFormateada = tokens;
    }

    @Override
    public void convertirPrefija() {
        // Limpia la lista prefija de conversiones anteriores
        prefija.clear();
        // Paso 1: invierte la lista de tokens y cambia paréntesis
        Collections.reverse(expresionFormateada);
        for (int i = 0; i < expresionFormateada.size(); i++) {
            String t = expresionFormateada.get(i);
            if (t.equals("(")) expresionFormateada.set(i, ")");
            else if (t.equals(")")) expresionFormateada.set(i, "(");
        }

        Stack<String> stack = new Stack<>();
        // Paso 2: aplicar algoritmo shunting-yard sobre la lista invertida
        for (String token : expresionFormateada) {
            if (token.matches("\\d+") || token.matches("[a-zA-Z]+")) {
                // Si es número o identificador, agregar directamente a la salida
                prefija.add(token);
            } else if (token.equals("(")) {
                // Paréntesis de apertura → push
                stack.push(token);
            } else if (token.equals(")")) {
                // Paréntesis de cierre → pop hasta encontrar “(”
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    prefija.add(stack.pop());
                }
                if (!stack.isEmpty()) stack.pop(); // Remover “(”
            } else {
                // Operador: pop mientras la prioridad sea mayor en la pila
                while (!stack.isEmpty() && !stack.peek().equals("(")
                        && prioridad(token) < prioridad(stack.peek())) {
                    prefija.add(stack.pop());
                }
                stack.push(token);
            }
        }
        // Vaciar pila restante
        while (!stack.isEmpty()) {
            prefija.add(stack.pop());
        }
        // Finalmente, revertir la salida para obtener la notación prefija correcta
        Collections.reverse(prefija);
    }

    @Override
    public void convertirPostfija() {
        postfija.clear();
        Stack<String> stack = new Stack<>();
        // Algoritmo shunting-yard clásico para notación postfija
        for (String token : expresionFormateada) {
            if (token.matches("\\d+") || token.matches("[a-zA-Z]+")) {
                // Números o variables van directo a la salida
                postfija.add(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                // Pop hasta encontrar “(”
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    postfija.add(stack.pop());
                }
                if (!stack.isEmpty()) stack.pop();
            } else {
                // Operador: pop según prioridad
                while (!stack.isEmpty() && !stack.peek().equals("(")
                        && prioridad(token) <= prioridad(stack.peek())) {
                    postfija.add(stack.pop());
                }
                stack.push(token);
            }
        }
        // Vaciar pila restante
        while (!stack.isEmpty()) {
            postfija.add(stack.pop());
        }
    }

    @Override
    public int parser(){
        // Evaluar la notación prefija
        Stack<Integer> stack = new Stack<>();
        for (int i = prefija.size() - 1; i >= 0; i--) {
            String token = prefija.get(i);
            if (token.matches("\\d+")) {
                // Si es número, convertir y push
                stack.push(Integer.parseInt(token));
            } else if (variables.containsKey(token)) {
                // Si es variable, recuperar su valor
                stack.push(variables.get(token));
            } else {
                // Operador → pop de operandos y aplicar operación
                int a = stack.pop();
                int b = stack.pop();
                int result;
                switch (token) {
                    case "+": result = a + b; break;
                    case "-": result = a - b; break;
                    case "*": result = a * b; break;
                    case "/":
                        if (b == 0)
                            throw new ArithmeticException("División por cero");
                        result = a / b;
                        break;
                    case "^":
                        result = (int) Math.pow(a, b);
                        break;
                    default:
                        throw new RuntimeException("Operador desconocido: " + token);
                }
                // Imprimir paso intermedio
                System.out.println(a + " " + token + " " + b + " = " + result);
                stack.push(result);
            }
        }

        // El resultado final queda en la pila
        int salida = stack.pop();

        // Si había asignación, guardarla en el mapa de variables
        if (variableAsignacion != null) {
            variables.put(variableAsignacion, salida);
            System.out.println("Variable asignada: " + variableAsignacion + " = " + salida);
            System.out.println("Variables: " + variables);
        }

        return salida;
    }

    // Método auxiliar para determinar prioridad de operadores
    private int prioridad(String op) {
        switch (op) {
            case "^": return 3;
            case "*": case "/": return 2;
            case "+": case "-": return 1;
            default: return 0;
        }
    }

    public String getExpresion() {
        return expresion;
    }
    public List<String> getPostfija(){
        return  postfija;
    }
    public List<String> getPrefija(){
        return  prefija;
    }
}

// Clase Main con bucle para leer líneas de la entrada estándar y procesarlas
public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Programa programa = new Programa();
        while (in.hasNextLine()) {
            String linea = in.nextLine();
            if (linea.isBlank()) break;
            try {
                programa.tokenizador(linea);
                programa.convertirPrefija();
                programa.convertirPostfija();
                programa.parser();
                System.out.println("Prefija: "+ programa.getPrefija());
                System.out.println("Postfija: "+ programa.getPostfija());
            } catch (RuntimeException e) {
                // Captura errores de parsing, operadores desconocidos, división por cero, etc.
                System.out.println("Expresión inválida");

            }
        }
    }
}