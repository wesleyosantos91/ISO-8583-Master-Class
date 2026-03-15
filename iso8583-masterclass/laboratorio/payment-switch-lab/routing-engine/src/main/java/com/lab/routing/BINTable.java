package com.lab.routing;

import java.util.TreeMap;
import java.util.Map;

/**
 * Tabela de BINs para roteamento.
 * Suporta lookup por 8 dígitos (padrão) com fallback para 6 dígitos.
 *
 * EXERCÍCIO SEMANA 11: Implementar lookup com longest prefix match.
 */
public class BINTable {

    private final TreeMap<String, Route> routes = new TreeMap<>();

    public void addRoute(String binPrefix, Route route) {
        routes.put(binPrefix, route);
    }

    /**
     * Busca rota pelo BIN. Tenta 8 dígitos, fallback para 6.
     * @param pan PAN completo
     * @return Route ou null se não encontrar
     */
    public Route lookup(String pan) {
        // TODO Semana 11:
        // 1. Extrair 8 dígitos do PAN
        // 2. Buscar na tabela (longest prefix match)
        // 3. Se não encontrar, tentar com 6 dígitos
        // 4. Se não encontrar, retornar null

        throw new UnsupportedOperationException("Implementar na Semana 11");
    }

    public int size() {
        return routes.size();
    }

    /**
     * Carrega BINs de um CSV.
     * Formato: bin_prefix,issuer_name,network,on_us,mux_name
     */
    public static BINTable fromCSV(String csvPath) {
        // TODO Semana 11: Implementar leitura de CSV
        throw new UnsupportedOperationException("Implementar na Semana 11");
    }
}
