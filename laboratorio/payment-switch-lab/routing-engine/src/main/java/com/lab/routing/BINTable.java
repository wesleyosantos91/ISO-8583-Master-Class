package com.lab.routing;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

/**
 * Tabela de BINs para roteamento.
 * Suporta lookup por 8 digitos (padrao) com fallback para 6 digitos.
 *
 * Referencia: Semana 11 — Roteamento por BIN.
 */
public class BINTable {

    private final TreeMap<String, Route> routes = new TreeMap<>();

    public void addRoute(String binPrefix, Route route) {
        routes.put(binPrefix, route);
    }

    /**
     * Busca rota pelo BIN. Tenta 8 digitos, fallback para 6.
     * Usa longest prefix match via TreeMap.floorEntry.
     *
     * @param pan PAN completo
     * @return Route ou null se nao encontrar
     */
    public Route lookup(String pan) {
        if (pan == null || pan.length() < 6) {
            return null;
        }

        // Tenta 8 digitos primeiro
        if (pan.length() >= 8) {
            String bin8 = pan.substring(0, 8);
            Route route = findByPrefix(bin8);
            if (route != null) {
                return route;
            }
        }

        // Fallback para 6 digitos
        String bin6 = pan.substring(0, 6);
        return findByPrefix(bin6);
    }

    private Route findByPrefix(String bin) {
        // Busca exata primeiro
        Route exact = routes.get(bin);
        if (exact != null) {
            return exact;
        }

        // Longest prefix match via floorEntry
        var entry = routes.floorEntry(bin);
        if (entry != null && bin.startsWith(entry.getKey())) {
            return entry.getValue();
        }

        return null;
    }

    public int size() {
        return routes.size();
    }

    /**
     * Carrega BINs de um CSV.
     * Formato: bin_prefix,issuer_name,network,on_us,mux_name,fallback_mux
     */
    public static BINTable fromCSV(String csvPath) {
        BINTable table = new BINTable();
        try (BufferedReader reader = Files.newBufferedReader(Path.of(csvPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length < 5) {
                    continue;
                }
                String binPrefix = parts[0].trim();
                String issuerName = parts[1].trim();
                String network = parts[2].trim();
                boolean onUs = Boolean.parseBoolean(parts[3].trim());
                String muxName = parts[4].trim();
                String fallbackMux = parts.length > 5 ? parts[5].trim() : null;
                if (fallbackMux != null && fallbackMux.isEmpty()) {
                    fallbackMux = null;
                }

                Route route = new Route(binPrefix, issuerName, network, onUs, muxName, fallbackMux);
                table.addRoute(binPrefix, route);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar BINTable de " + csvPath, e);
        }
        return table;
    }
}
