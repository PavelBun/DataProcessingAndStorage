package server;

public record KeyRequest(String name, ClientSession session) {
    public String getName() { return name; }
    public ClientSession getSession() { return session; }
}