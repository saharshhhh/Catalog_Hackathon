import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SecretReconstructionGson {

    // Set precision for BigDecimal calculations (adjust if needed)
    private static final MathContext MATH_CONTEXT = new MathContext(50);

    public static void main(String[] args) {
        try {
            // Step 1: Parse JSON with Gson
            JsonReader reader = new JsonReader(new FileReader("src/main/resources/input.json"));
            JsonObject rootObject = JsonParser.parseReader(reader).getAsJsonObject();

            // Step 2: Read n and k
            JsonObject keys = rootObject.getAsJsonObject("keys");
            int n = keys.get("n").getAsInt();
            int k = keys.get("k").getAsInt();

            // Step 3: Extract roots (x, y) with y decoded as BigInteger
            List<Root> roots = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : rootObject.entrySet()) {
                String key = entry.getKey();
                if (key.equals("keys") || !key.matches("\\d+")) continue;

                int x = Integer.parseInt(key);
                JsonObject rootData = entry.getValue().getAsJsonObject();

                int base = Integer.parseInt(rootData.get("base").getAsString());
                String valueStr = rootData.get("value").getAsString();

                // Use BigInteger to decode large y
                BigInteger y = new BigInteger(valueStr, base);

                roots.add(new Root(x, y));
            }

            // Optionally sort by x
            roots.sort((r1, r2) -> Integer.compare(r1.x, r2.x));

            // Print parsed roots
            System.out.println("Parsed roots (x, y):");
            for (Root r : roots) {
                System.out.printf("x = %d, y = %s%n", r.x, r.y.toString());
            }

            if (roots.size() < k) {
                System.err.println("Not enough roots to solve for the secret (need at least k)");
                return;
            }

            // Step 4: Lagrange interpolation to reconstruct secret at x=0
            BigDecimal secret = lagrangeInterpolationAtZero(roots, k);

            // Round the secret to nearest whole number (BigInteger)
            BigInteger secretInt = secret.setScale(0, BigDecimal.ROUND_HALF_UP).toBigIntegerExact();

            System.out.println("Secret (c) = " + secretInt.toString());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lagrange interpolation at x=0 using first k roots.
     * Uses BigDecimal for precision.
     *
     * @param roots List of roots (x,y)
     * @param k     number of points to use
     * @return interpolated value y(0) as BigDecimal
     */
    private static BigDecimal lagrangeInterpolationAtZero(List<Root> roots, int k) {
        BigDecimal result = BigDecimal.ZERO;

        for (int i = 0; i < k; i++) {
            BigDecimal term = new BigDecimal(roots.get(i).y, MATH_CONTEXT);
            int x_i = roots.get(i).x;

            for (int j = 0; j < k; j++) {
                if (i != j) {
                    int x_j = roots.get(j).x;

                    BigDecimal numerator = BigDecimal.valueOf(0 - x_j);
                    BigDecimal denominator = BigDecimal.valueOf(x_i - x_j);
                    // term *= (0 - x_j) / (x_i - x_j)
                    term = term.multiply(numerator.divide(denominator, MATH_CONTEXT), MATH_CONTEXT);
                }
            }
            result = result.add(term, MATH_CONTEXT);
        }

        return result;
    }

    // Simple container class for a root
    static class Root {
        int x;
        BigInteger y;

        Root(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }
}
