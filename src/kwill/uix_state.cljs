(ns kwill.uix-state
  (:require
    [uix.core :as uix]))

(defonce *registry (atom {}))

(defn reg!
  [& {:keys [type id handler]}]
  (swap! *registry assoc [type id] handler))

(defn get-handler
  [& {:keys [type id]}]
  (get @*registry [type id]))

(defn reg-event!
  [id opts handler]
  (reg! :type :event :id id :handler handler))

(defn reg-sub!
  [id opts handler]
  (reg! :type :sub :id id :handler handler))

(defn create-context-value
  [_]
  (let [[db dispatch] (uix/use-reducer
                        (fn [db event]
                          (if-let [handler (get-handler :type :event :id (:id event))]
                            (let [{:keys [db]} (handler {:db db} event)]
                              db)
                            (js/console.warn (str "Missing event handler for event " (:id event)))))
                        {})]
    {:db db :disp dispatch}))

(defonce AppDb (uix/create-context))

(uix/defui AppDbProvider
  [{:keys [children]}]
  (uix/$ (.-Provider AppDb) {:value (create-context-value nil)}
    children))

(defn use-app-db
  []
  (uix/use-context AppDb))

(defn sub
  ([id] (sub id nil))
  ([id argm]
   (let [{:keys [db]} (use-app-db)
         f (get-handler :type :sub :id id)
         _ (when-not f (throw (ex-info (str "Subscription handler " id " not found.")
                                {:id id})))
         v (f {:db db} argm)]
     v)))

(defn disp
  ([event])
  ([a event]))
