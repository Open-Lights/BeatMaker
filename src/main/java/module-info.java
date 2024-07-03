module BeatMaker.main {
    requires java.logging;
    requires imgui.app;
    requires imgui.binding;
    requires java.desktop;
    requires com.google.gson;
    requires TarsosDSP.jvm;
    requires TarsosDSP.core;


    exports com.github.qpcrummer.beatmaker;
}