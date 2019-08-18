/**
 * MIME-Type Parser
 *
 * This class provides basic functions for handling mime-types. It can handle
 * matching mime-types against a list of media-ranges. See section 14.1 of the
 * HTTP specification [RFC 2616] for a complete explanation.
 *
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1
 *
 * A port to Java of Joe Gregorio's MIME-Type Parser:
 *
 * http://code.google.com/p/mimeparse/
 *
 * Ported by Tom Zellman <tzellman@gmail.com>.
 */

package ratpack.http.internal;

import java.util.*;

public final class MimeParse {

  protected static class ParseResults {
    String type;

    String subType;

    // !a dictionary of all the parameters for the media range
    Map<String, String> params;

    @Override
    public String toString() {
      StringBuilder s = new StringBuilder("('" + type + "', '" + subType
        + "', {");
      for (String k : params.keySet()) {
        s.append("'").append(k).append("':'").append(params.get(k)).append("',");
      }
      return s.append("})").toString();
    }
  }

  protected static ParseResults parseMimeType(CharSequence mimeType) {
    String[] parts = mimeType.toString().split(";");
    ParseResults results = new ParseResults();
    results.params = new HashMap<>();

    String fullType = parts[0].trim();

    // Java URLConnection class sends an Accept header that includes a
    // single "*" - Turn it into a legal wildcard.
    if (fullType.equals("*")) {
      fullType = "*/*";
    }
    String[] types = fullType.split("/", 2);
    if (types.length != 2) {
      results.type = types[0].trim();
      return results;
    }

    results.type = types[0].trim();
    results.subType = types[1].trim();

    for (int i = 1; i < parts.length; ++i) {
      String p = parts[i];
      String[] subParts = p.split("=", 2);
      if (subParts.length == 2) {
        results.params.put(subParts[0].trim(), subParts[1].trim());
      }
    }

    return results;
  }

  protected static ParseResults parseMediaRange(CharSequence range) {
    ParseResults results = parseMimeType(range);
    String q = results.params.get("q");
    float f = toFloat(q, -1);
    if (q == null || q.trim().isEmpty() || f < 0 || f > 1) {
      results.params.put("q", "1");
    }
    return results;
  }

  /**
   * Structure for holding a fitness/quality combo
   */
  protected static class FitnessAndQuality implements
    Comparable<FitnessAndQuality> {
    int fitness;

    float quality;

    String mimeType; // optionally used

    public FitnessAndQuality(int fitness, float quality) {
      this.fitness = fitness;
      this.quality = quality;
    }

    public int compareTo(FitnessAndQuality o) {
      if (fitness == o.fitness) {
        if (quality == o.quality) {
          return 0;
        } else {
          return quality < o.quality ? -1 : 1;
        }
      } else {
        return fitness < o.fitness ? -1 : 1;
      }
    }
  }

  protected static FitnessAndQuality fitnessAndQualityParsed(CharSequence mimeType,
                                                             Collection<ParseResults> parsedRanges) {
    int bestFitness = -1;
    float bestFitQ = 0;
    ParseResults target = parseMediaRange(mimeType);

    for (ParseResults range : parsedRanges) {

      boolean typeMatch = target.type.equals(range.type) || range.type.equals("*") || target.type.equals("*");
      boolean subTypeMatch;
      if (target.subType == null || range.subType == null) {
        subTypeMatch = Objects.equals(target.subType, range.subType);
      } else {
        subTypeMatch = target.subType.equals(range.subType) || range.subType.equals("*") || target.subType.equals("*");
      }

      if (typeMatch && subTypeMatch) {
        for (String k : target.params.keySet()) {
          int paramMatches = 0;
          if (!k.equals("q") && range.params.containsKey(k)
            && target.params.get(k).equals(range.params.get(k))) {
            paramMatches++;
          }
          int fitness = (range.type.equals(target.type)) ? 100 : 0;
          fitness += (Objects.equals(range.subType, target.subType)) ? 10 : 0;
          fitness += paramMatches;
          if (fitness > bestFitness) {
            bestFitness = fitness;
            bestFitQ = toFloat(range.params.get("q"), 0);
          }
        }
      }
    }
    return new FitnessAndQuality(bestFitness, bestFitQ);
  }

  public static String bestMatch(Iterable<? extends CharSequence> supported, String header) {
    List<ParseResults> parseResults = new ArrayList<>();
    List<FitnessAndQuality> weightedMatches = new ArrayList<>();
    for (String r : header.split(",")) {
      ParseResults parseResult = parseMediaRange(r);
      if (parseResult != null) {
        parseResults.add(parseResult);
      }
    }

    for (CharSequence s : supported) {
      FitnessAndQuality fitnessAndQuality = fitnessAndQualityParsed(s, parseResults);
      fitnessAndQuality.mimeType = s.toString();
      weightedMatches.add(fitnessAndQuality);
    }
    Collections.sort(weightedMatches);

    FitnessAndQuality lastOne = weightedMatches.get(weightedMatches.size() - 1);
    return lastOne.quality != 0f ? lastOne.mimeType : "";
  }

  private static float toFloat(String f, float ifNullOrNotANumber) {
    try {
      return f == null ? ifNullOrNotANumber : Float.valueOf(f);
    } catch (NumberFormatException e) {
      return ifNullOrNotANumber;
    }
  }

  // hidden
  private MimeParse() {
  }
}
