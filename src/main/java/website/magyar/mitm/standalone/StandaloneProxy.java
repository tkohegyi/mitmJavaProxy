package website.magyar.mitm.standalone;

public final class StandaloneProxy {
    public static String[] arguments; //NOSONAR

    private StandaloneProxy() {
    }

    /**
     * The app main entry point.
     * @param args The program needs the path of conf.properties to run.
     */
    public static void main(final String[] args) {
        arguments = args; //NOSONAR
        new Bootstrap().bootstrap(args);
    }
}
