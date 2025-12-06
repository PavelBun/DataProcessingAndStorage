(defn notify-msg
  "A message that can be sent to a factory worker to notify that the provided 'amount' of 'ware's are
   just put to the 'storage-atom'."
  [state ware storage-atom amount]
  (let [bill (:bill state)
        buffer (:buffer state)]
    ;; Проверяем, нужен ли этот товар для производства
    (if-let [needed-amount (get bill ware)]
      ;; Вычисляем, сколько можно забрать
      (let [can-take (min needed-amount amount)
            new-buffer (update buffer ware #(min needed-amount (+ % can-take)))]
        ;; Пытаемся забрать товар со склада
        (try
          (swap! storage-atom #(if (>= % can-take) (- % can-take) (throw (IllegalStateException.))))
          ;; Если удалось забрать, обновляем состояние
          (let [updated-state (assoc state :buffer new-buffer)]
            ;; Проверяем, достаточно ли товаров для производства
            (if (every? (fn [[w req]] (>= (get new-buffer w 0) req)) bill)
              ;; Если достаточно - начинаем производственный цикл
              (do
                (future
                  (Thread/sleep (:duration state))
                  ;; После завершения цикла очищаем буфер и уведомляем целевой склад
                  (let [final-state (assoc updated-state :buffer (reduce-kv 
                                                                   (fn [buf k _] (assoc buf k 0)) 
                                                                   {} 
                                                                   bill))]
                    (send (:worker (:target-storage state)) supply-msg (:amount state))
                    final-state))
                ;; Возвращаем состояние с обнуленным буфером
                (assoc updated-state :buffer (reduce-kv 
                                               (fn [buf k _] (assoc buf k 0)) 
                                               {} 
                                               bill)))
              ;; Если недостаточно - возвращаем обновленный буфер
              updated-state))
          (catch IllegalStateException e
            ;; Если не удалось забрать (например, недостаточно товара), 
            ;; возвращаем исходное состояние
            state)))
      ;; Если товар не нужен для производства, возвращаем состояние без изменений
      state)))
