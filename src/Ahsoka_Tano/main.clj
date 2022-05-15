(ns Ahsoka-Tano.main
  (:require
   [clojure.core.async :as Little-Rock
    :refer [chan put! take! close! offer! to-chan! timeout thread
            sliding-buffer dropping-buffer
            go >! <! alt! alts! do-alts
            mult tap untap pub sub unsub mix unmix admix
            pipe pipeline pipeline-async]]
   [clojure.core.async.impl.protocols :refer [closed?]]
   [clojure.java.io :as Wichita.java.io]
   [clojure.string :as Wichita.string]
   [clojure.pprint :as Wichita.pprint]
   [clojure.repl :as Wichita.repl]
   [cljfmt.core :as Joker.core]

   [aleph.http :as Simba.http]

   [Ahsoka-Tano.seed]
   [Ahsoka-Tano.oranges]
   [Ahsoka-Tano.salt]
   [Ahsoka-Tano.microwaved-onions]
   [Ahsoka-Tano.corn])
  (:import
   (javax.swing JFrame WindowConstants ImageIcon JPanel JScrollPane JTextArea BoxLayout JEditorPane ScrollPaneConstants SwingUtilities JDialog)
   (javax.swing JMenu JMenuItem JMenuBar KeyStroke JOptionPane JToolBar JButton JToggleButton JSplitPane JTextPane)
   (javax.swing.border EmptyBorder)
   (java.awt Canvas Graphics Graphics2D Shape Color Polygon Dimension BasicStroke Toolkit Insets BorderLayout)
   (java.awt.event KeyListener KeyEvent MouseListener MouseEvent ActionListener ActionEvent ComponentListener ComponentEvent )
   (java.awt.event  WindowListener WindowAdapter WindowEvent)
   (java.awt.geom Ellipse2D Ellipse2D$Double Point2D$Double)
   (com.formdev.flatlaf FlatLaf FlatLightLaf)
   (com.formdev.flatlaf.extras FlatUIDefaultsInspector FlatDesktop FlatDesktop$QuitResponse FlatSVGIcon)
   (com.formdev.flatlaf.util SystemInfo UIScale)
   (java.util.function Consumer)
   (java.util ServiceLoader)
   (org.kordamp.ikonli Ikon)
   (org.kordamp.ikonli IkonProvider)
   (org.kordamp.ikonli.swing FontIcon)
   (org.kordamp.ikonli.codicons Codicons)
   (net.miginfocom.swing MigLayout)
   (net.miginfocom.layout ConstraintParser LC UnitValue)
   (java.io File)
   (java.lang Runnable))
  (:gen-class))

(do (set! *warn-on-reflection* true) (set! *unchecked-math* true))

(defonce stateA (atom nil))
(defonce gamesA (atom nil))
(defonce gameA (atom nil))
(defonce settingsA (atom nil))

(defonce resize| (chan (sliding-buffer 1)))
(defonce eval| (chan 10))
(defonce cancel-sub| (chan 1))
(defonce cancel-pub| (chan 1))
(defonce ops| (chan 10))
(defonce table| (chan (sliding-buffer 10)))
(defonce sub| (chan (sliding-buffer 10)))
(defonce server| (chan 1))
(def ^:const jframe-title "surrender - or face the consequences")
(def ^:dynamic ^JFrame jframe nil)


(defn reload
  []
  (require
   '[Ahsoka-Tano.seed]
   '[Ahsoka-Tano.oranges]
   '[Ahsoka-Tano.salt]
   '[Ahsoka-Tano.microwaved-onions]
   '[Ahsoka-Tano.corn]
   '[Ahsoka-Tano.main]
   :reload))

(defn -main
  [& args]
  (println "i dont want my next job")

  #_(alter-var-root #'*ns* (constantly (find-ns 'Ahsoka-Tano.main)))

  (when SystemInfo/isMacOS
    (System/setProperty "apple.laf.useScreenMenuBar" "true")
    (System/setProperty "apple.awt.application.name" jframe-title)
    (System/setProperty "apple.awt.application.appearance" "system"))

  (when SystemInfo/isLinux
    (JFrame/setDefaultLookAndFeelDecorated true)
    (JDialog/setDefaultLookAndFeelDecorated true))

  (when (and
         (not SystemInfo/isJava_9_orLater)
         (= (System/getProperty "flatlaf.uiScale") nil))
    (System/setProperty "flatlaf.uiScale" "2x"))

  (FlatLightLaf/setup)

  (FlatDesktop/setQuitHandler (reify Consumer
                                (accept [_ response]
                                  (.performQuit ^FlatDesktop$QuitResponse response))
                                (andThen [_ after] after)))

  (let [screenshotsMode? (Boolean/parseBoolean (System/getProperty "flatlaf.demo.screenshotsMode"))

        jframe (JFrame. jframe-title)
        jmenubar (JMenuBar.)
        jroot-panel (JPanel.)]

    (let [data-dir-path (or
                         (some-> (System/getenv "AHSOKA_TANO_PATH")
                                 (.replaceFirst "^~" (System/getProperty "user.home")))
                         (.getCanonicalPath ^File (Wichita.java.io/file (System/getProperty "user.home") "Ahsoka-Tano")))
          state-file-path (.getCanonicalPath ^File (Wichita.java.io/file data-dir-path "Ahsoka-Tano.edn"))]
      (Wichita.java.io/make-parents data-dir-path)
      (reset! stateA {})
      (reset! settingsA {}))

    (SwingUtilities/invokeLater
     (reify Runnable
       (run [_]

         (doto jframe
           (.add jroot-panel)
           (.addComponentListener (let []
                                    (reify ComponentListener
                                      (componentHidden [_ event])
                                      (componentMoved [_ event])
                                      (componentResized [_ event] (put! resize| (.getTime (java.util.Date.))))
                                      (componentShown [_ event]))))
           (.addWindowListener (proxy [WindowAdapter] []
                                 (windowClosing [event]
                                   (let [event ^WindowEvent event]
                                     #_(println :window-closing)
                                     (put! server| true)
                                     (-> event (.getWindow) (.dispose)))))))

         (doto jroot-panel
           #_(.setLayout (BoxLayout. jroot-panel BoxLayout/Y_AXIS))
           (.setLayout (MigLayout. "insets 10"
                                   "[grow,shrink,fill]"
                                   "[grow,shrink,fill]")))

         (when-let [url (Wichita.java.io/resource "icon.png")]
           (.setIconImage jframe (.getImage (ImageIcon. url))))

         (Ahsoka-Tano.oranges/menubar-process
          {:jmenubar jmenubar
           :jframe jframe
           :menubar| ops|})
         (.setJMenuBar jframe jmenubar)


         (.setPreferredSize jframe
                            (let [size (-> (Toolkit/getDefaultToolkit) (.getScreenSize))]
                              (Dimension. (UIScale/scale 1024) (UIScale/scale 576)))
                            #_(if SystemInfo/isJava_9_orLater
                                (Dimension. 830 440)
                                (Dimension. 1660 880)))

         #_(doto jframe
             (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE #_WindowConstants/EXIT_ON_CLOSE)
             (.setSize 2400 1600)
             (.setLocation 1300 200)
             #_(.add panel)
             (.setVisible true))

         #_(println :before (.getGraphics canvas))
         (doto jframe
           (.setDefaultCloseOperation WindowConstants/DISPOSE_ON_CLOSE #_WindowConstants/EXIT_ON_CLOSE)
           (.pack)
           (.setLocationRelativeTo nil)
           (.setVisible true))
         #_(println :after (.getGraphics canvas))

         (alter-var-root #'Ahsoka-Tano.main/jframe (constantly jframe))

         (remove-watch stateA :watch-fn)
         (add-watch stateA :watch-fn
                    (fn [ref wathc-key old-state new-state]

                      (when (not= old-state new-state)
                        )))

         (remove-watch settingsA :main)
         (add-watch settingsA :main
                    (fn [ref wathc-key old-state new-state]
                      (SwingUtilities/invokeLater
                       (reify Runnable
                         (run [_])))))
         (reset! settingsA @settingsA))))

    (let [port (or (System/getenv "AHSOKA_TANO_IPFS_PORT") "5001")
          ipfs-api-url (format "http://127.0.0.1:%s" port)
          id| (chan 1)]

      (Ahsoka-Tano.corn/subscribe-process
       {:sub| sub|
        :cancel| (chan (sliding-buffer 1))
        :frequency "raisins"
        :ipfs-api-url ipfs-api-url
        :ipfs-api-multiaddress (format "/ip4/127.0.0.1/tcp/%s" port)
        :id| id|}))

    (let [port (or (try (Integer/parseInt (System/getenv "PORT"))
                        (catch Exception e nil))
                   3355)]
      (Ahsoka-Tano.microwaved-onions/process
       {:port port
        :server| server|})))
  (println "Kuiil has spoken"))