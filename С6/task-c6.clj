(ns task-c6
  (:require [clojure.set :as set]
            [clojure.data.priority-map :refer [priority-map]]))

;;; Монитор перезапусков транзакций
(def transact-cnt (atom 0))

(defn- dijkstra
  "Находит кратчайший путь от from до to в графе route-map.
   Возвращает {:path путь, :price стоимость, :edges список рёбер} или nil"
  [route-map from to]
  (let [graph (:forward route-map)]
    (when (and (contains? graph from) (contains? graph to))
      (loop [distances (priority-map from 0)
             prev {from nil}
             visited #{}]
        (if-let [[current curr-dist] (peek distances)]
          (if (= current to)
            ;; Восстанавливаем путь
            (let [path (reverse (take-while identity (iterate prev current)))
                  edges (partition 2 1 path)
                  total-price curr-dist]
              {:path path, :price total-price, :edges edges})
            (let [neighbors (set/difference (keys (get graph current {})) visited)
                  new-distances
                  (reduce (fn [dist n]
                            (let [edge (get-in graph [current n])
                                  new-dist (+ curr-dist (:price edge))]
                              (if (or (not (contains? dist n))
                                      (< new-dist (dist n)))
                                (assoc dist n new-dist)
                                dist)))
                          (pop distances)
                          neighbors)
                  new-prev
                  (reduce (fn [p n]
                            (if (or (not (contains? p n))
                                    (< (+ curr-dist (get-in graph [current n :price])) 
                                       (distances n)))
                              (assoc p n current)
                              p))
                          prev
                          neighbors)]
              (recur new-distances new-prev (conj visited current))))
          nil)))))

(defn book-tickets
  "Пытается забронировать билеты и атомарно уменьшить соответствующие ссылки в route-map.
   Возвращает map либо с :price (за весь маршрут) и :path (список названий пунктов),
   либо с :error, если бронирование невозможно из-за нехватки билетов."
  [route-map from to]
  (if (= from to)
    {:path '(), :price 0}
    (let [result (ref nil)]
      (dosync
        (swap! transact-cnt inc)
        (if-let [{:keys [path price edges]} (dijkstra route-map from to)]
          ;; Проверяем доступность всех билетов
          (let [ticket-refs (map (fn [[from-node to-node]]
                                   (get-in route-map [:forward from-node to-node :tickets]))
                                 edges)
                all-available? (every? #(>= @% 1) ticket-refs)]
            (if all-available?
              (do
                ;; Атомарно уменьшаем количество билетов
                (doseq [ticket-ref ticket-refs]
                  (alter ticket-ref dec))
                (ref-set result {:path path, :price price}))
              (ref-set result {:error "Not enough tickets"})))
          (ref-set result {:error "No path found"}))))
      @result)))

;;; Пример использования с тестовыми данными
(comment
  (def spec1 (-> empty-map
               (route "City1" "Capital"    200 5)
               (route "Capital" "City1"    250 5)
               (route "City2" "Capital"    200 5)
               (route "Capital" "City2"    250 5)
               (route "City3" "Capital"    300 3)
               (route "Capital" "City3"    400 3)
               (route "City1" "Town1_X"    50 2)
               (route "Town1_X" "City1"    150 2)
               (route "Town1_X" "TownX_2"  50 2)
               (route "TownX_2" "Town1_X"  150 2)
               (route "Town1_X" "TownX_2"  50 2)
               (route "TownX_2" "City2"    50 3)
               (route "City2" "TownX_2"    150 3)
               (route "City2" "Town2_3"    50 2)
               (route "Town2_3" "City2"    150 2)
               (route "Town2_3" "City3"    50 3)
               (route "City3" "Town2_3"    150 2)))

  ;; Тест поиска пути
  (dijkstra spec1 "City1" "City3")
  ;; => {:path ["City1" "Town1_X" "TownX_2" "City2" "Town2_3" "City3"], 
  ;;     :price 250, 
  ;;     :edges (["City1" "Town1_X"] ["Town1_X" "TownX_2"] 
  ;;             ["TownX_2" "City2"] ["City2" "Town2_3"] ["Town2_3" "City3"])}

  ;; Тест бронирования
  (book-tickets spec1 "City1" "City3")
  ;; => {:path ["City1" "Town1_X" "TownX_2" "City2" "Town2_3" "City3"], :price 250}

  ;; Проверка количества билетов после бронирования
  (doseq [[from to] [["City1" "Town1_X"] ["Town1_X" "TownX_2"] 
                     ["TownX_2" "City2"] ["City2" "Town2_3"] ["Town2_3" "City3"]]]
    (println from "->" to ":" @(get-in spec1 [:forward from to :tickets])))
  ;; После первого бронирования все значения уменьшатся на 1
  )

(defn booking-future [route-map from to init-delay loop-delay]
  (future 
    (Thread/sleep init-delay) 
    (loop [bookings []]
      (Thread/sleep loop-delay)
      (let [booking (book-tickets route-map from to)]
        (if (booking :error)
          bookings
          (recur (conj bookings booking)))))))

(defn print-bookings [name ft]
  (println (str name ":") (count ft) "bookings")
  (doseq [booking ft]
    (println "  price:" (booking :price) "path:" (booking :path))))

(defn run []
  ;; Подбираем задержки для удовлетворения всех клиентов
  ;; Увеличение задержек уменьшает конфликты
  (let [f1 (booking-future spec1 "City1" "City3" 0 50)    ; первым начинает
        f2 (booking-future spec1 "City1" "City2" 10 60)   ; небольшая задержка
        f3 (booking-future spec1 "City2" "City3" 20 70)]  ; ещё большая задержка
    
    ;; Ждём завершения всех futures
    (Thread/sleep 1000)
    
    (print-bookings "City1->City3" @f1)
    (print-bookings "City1->City2" @f2)
    (print-bookings "City2->City3" @f3)
    
    ;; Выводим количество перезапусков транзакций
    (println "Total transaction (re-)starts:" @transact-cnt)))

;;; Запуск теста
(comment
  (run)
  ;; Пример вывода:
  ;; City1->City3: 3 bookings
  ;;   price: 250 path: [City1 Town1_X TownX_2 City2 Town2_3 City3]
  ;;   ...
  ;; City1->City2: 2 bookings
  ;;   ...
  ;; City2->City3: 1 bookings
  ;;   ...
  ;; Total transaction (re-)starts: 42
  )
