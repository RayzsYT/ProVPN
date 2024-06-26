package de.rayzs.provpn.utils.builder;

import de.rayzs.provpn.plugin.logger.Logger;
import java.util.Scanner;
import java.net.*;

public class ConnectionBuilder {

    private String url = null, response = null;
    private Object[] parameters = null;

    public ConnectionBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    public ConnectionBuilder setProperties(Object... parameters) {
        this.parameters = parameters;
        return this;
    }

    public ConnectionBuilder connect() {
        try {

            String rawUrl = url;
            URL url = new URL(rawUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            if(parameters.length > 0) {
                Object firstParam = null, secondParam = null;
                for (Object parameter : parameters) {
                    if(firstParam == null) firstParam = parameter;
                    else if(secondParam == null) secondParam = parameter;
                    else {
                        connection.setRequestProperty((String) firstParam, (String) secondParam);
                        firstParam = null;
                        secondParam = null;
                    }
                }
            }

            Scanner scanner = new Scanner(connection.getInputStream());
            StringBuilder builder = new StringBuilder("\\");
            while (scanner.hasNextLine()) builder.append(" ").append(scanner.next());
            response = builder.toString().replace("\\ ", "");

        } catch (Exception exception) {
            Logger.warning("§cFailed to built connection to website! [" + exception.getMessage() + "]");
        }
        return this;
    }

    public boolean hasResponse() {
        return response != null;
    }

    public String getResponse() {
        return response;
    }
}