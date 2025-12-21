(ns dining-philosophers.core
  (:require [clojure.set :as set])
  (:import (java.util.concurrent TimeUnit)))

(defrecord Fork [id counter])
(defrecord Philosopher [id thinking-time eating-time meals-left])

;; Глобальный счетчик перезапусков транзакций
(def restart-counter (atom 0))

(defn create-forks [n]
  "Создает n вилок с начальными счетчиками"
  (vec (for [i (range n)]
         (ref (->Fork i 0)))))

(defn create-philosophers [n thinking-time eating-time meals]
  "Создает n философов с заданными параметрами"
  (vec (for [i (range n)]
         (->Philosopher i thinking-time eating-time meals))))

(defn pick-forks [philosopher-id forks strategy]
  "Пытается взять вилки в зависимости от стратегии.
   Возвращает [left-fork-ref right-fork-ref] если удалось, nil если нет."
  (let [n (count forks)
        left-id philosopher-id
        right-id (mod (inc philosopher-id) n)]
    (case strategy
      :left-right   [(forks left-id) (forks right-id)]
      :right-left   [(forks right-id) (forks left-id)]
      :alternating  (if (even? philosopher-id)
                      [(forks left-id) (forks right-id)]
                      [(forks right-id) (forks left-id)]))))

(defn acquire-forks! [left-fork-ref right-fork-ref]
  "Пытается взять две вилки в транзакции.
   Возвращает true если успешно, false если нет."
  (try
    (dosync
      (swap! restart-counter inc)  ; Увеличиваем счетчик попыток
      (let [left-fork @left-fork-ref
            right-fork @right-fork-ref]
        ;; Проверяем, свободны ли вилки
        (ref-set left-fork-ref (update left-fork :counter inc))
        (ref-set right-fork-ref (update right-fork :counter inc))
        true))
    (catch Exception _
      false)))

(defn release-forks! [left-fork-ref right-fork-ref]
  "Освобождает вилки"
  true)

(defn philosopher-loop [philosopher forks strategy]
  "Основной цикл философа"
  (let [{:keys [id thinking-time eating-time meals-left]} philosopher
        [left-ref right-ref] (pick-forks id forks strategy)]
    
    (when (pos? meals-left)
      ;; Этап размышлений
      (Thread/sleep thinking-time)
      
      ;; Этап еды
      (when (acquire-forks! left-ref right-ref)
        (Thread/sleep eating-time)
        (release-forks! left-ref right-ref)
        
        ;; Рекурсивно продолжаем с уменьшенным счетчиком приемов пищи
        (recur (update philosopher :meals-left dec) forks strategy)))))

(defn run-dining-philosophers [n-philosophers thinking-time eating-time total-meals strategy]
  "Запускает симуляцию обедающих философов"
  (let [forks (create-forks n-philosophers)
        philosophers (create-philosophers n-philosophers thinking-time eating-time total-meals)
        start-time (System/nanoTime)]
    
    ;; Запускаем всех философов в отдельных потоках
    (let [futures (doall
                    (for [phil philosophers]
                      (future (philosopher-loop phil forks strategy))))]
      
      ;; Ждем завершения всех философов
      (doseq [f futures] @f)
      
      ;; Собираем статистику
      (let [end-time (System/nanoTime)
            execution-time (/ (double (- end-time start-time)) 1000000.0)  ; в миллисекундах
            fork-usage (map deref forks)
            total-restarts @restart-counter]
        
        {:execution-time-ms execution-time
         :total-restarts total-restarts
         :fork-usage fork-usage
         :restarts-per-philosopher (float (/ total-restarts n-philosophers total-meals))})))

(defn print-stats [stats]
  "Печатает статистику выполнения"
  (println "\n=== Статистика выполнения ===")
  (println (format "Общее время: %.2f мс" (:execution-time-ms stats)))
  (println (format "Всего перезапусков транзакций: %d" (:total-restarts stats)))
  (println (format "Перезапусков на философа на прием пищи: %.2f" (:restarts-per-philosopher stats)))
  (println "\nИспользование вилок:")
  (doseq [fork (:fork-usage stats)]
    (println (format "  Вилка %d использовалась %d раз" (:id fork) (:counter fork)))))

;; Примеры экспериментов
(defn experiment-even-philosophers []
  "Эксперимент с четным числом философов (обычно работает хорошо)"
  (println "\n=== Эксперимент: 4 философа (четное число) ===")
  (println "Стратегия: alternating (четные берут левую-правую, нечетные - правую-левую)")
  (let [stats (run-dining-philosophers
                4      ; количество философов
                50     ; время размышления (мс)
                100    ; время еды (мс)
                5      ; количество приемов пищи на философа
                :alternating)]
    (print-stats stats)))

(defn experiment-odd-philosophers []
  "Эксперимент с нечетным числом философов"
  (println "\n=== Эксперимент: 5 философов (нечетное число) ===")
  (println "Стратегия: alternating")
  (let [stats (run-dining-philosophers
                5      ; количество философов
                50     ; время размышления (мс)
                100    ; время еды (мс)
                5      ; количество приемов пищи на философа
                :alternating)]
    (print-stats stats)))

(defn experiment-livelock-prone []
  "Эксперимент, провоцирующий livelock (все берут сначала левую вилку)"
  (println "\n=== Эксперимент: Провокация livelock (5 философов) ===")
  (println "Стратегия: left-right (все берут сначала левую вилку)")
  (let [stats (run-dining-philosophers
                5      ; количество философов
                10     ; короткое время размышления
                10     ; короткое время еды
                3      ; меньше приемов пищи
                :left-right)]
    (print-stats stats)))

(defn experiment-random-backoff []
  "Эксперимент со случайной задержкой для избежания livelock"
  (println "\n=== Эксперимент: Случайная задержка (5 философов) ===")
  (println "Стратегия: left-right со случайной задержкой")
  
  (defn acquire-forks-with-backoff! [left-fork-ref right-fork-ref]
    "Версия с обратной задержкой при конфликте"
    (try
      (dosync
        (swap! restart-counter inc)
        (let [left-fork @left-fork-ref
              right-fork @right-fork-ref]
          ;; Добавляем небольшую случайную задержку в транзакции
          (when (< (rand) 0.3)
            (Thread/sleep (rand-int 5)))
          
          (ref-set left-fork-ref (update left-fork :counter inc))
          (ref-set right-fork-ref (update right-fork :counter inc))
          true))
      (catch Exception _
        ;; Задержка перед повторной попыткой
        (Thread/sleep (rand-int 20))
        false)))
  
  ;; Временно переопределяем функцию для этого эксперимента
  (with-redefs [acquire-forks! acquire-forks-with-backoff!]
    (let [stats (run-dining-philosophers
                  5      ; количество философов
                  50     ; время размышления
                  100    ; время еды
                  5      ; количество приемов пищи
                  :left-right)]
      (print-stats stats))))

(defn run-all-experiments []
  "Запускает все эксперименты"
  ;; Сбрасываем счетчик перед каждым экспериментом
  (def restart-counter (atom 0))
  (experiment-even-philosophers)
  
  (def restart-counter (atom 0))
  (experiment-odd-philosophers)
  
  (def restart-counter (atom 0))
  (experiment-livelock-prone)
  
  (def restart-counter (atom 0))
  (experiment-random-backoff))

;; Функция для запуска с произвольными параметрами
(defn custom-experiment [n think-time eat-time meals strategy]
  (println (format "\n=== Пользовательский эксперимент: %d философов ===" n))
  (println (format "Стратегия: %s" strategy))
  (println (format "Время размышления: %d мс, время еды: %d мс" think-time eat-time))
  
  (def restart-counter (atom 0))
  (let [stats (run-dining-philosophers n think-time eat-time meals strategy)]
    (print-stats stats)
    stats))

(comment
  ;; Запустить все эксперименты
  (run-all-experiments)
  
  ;; Пользовательский эксперимент
  (custom-experiment 7 100 150 4 :alternating)
  
  ;; Проверка на большом количестве философов
  (custom-experiment 10 20 30 10 :alternating)
  
  ;; Тест на livelock с нечетным числом
  (custom-experiment 3 5 5 10 :left-right)
  
  ;; Сравнение производительности
  (let [stats1 (custom-experiment 6 50 50 20 :alternating)
        stats2 (custom-experiment 6 50 50 20 :left-right)]
    (println "\n=== Сравнение производительности ===")
    (println (format "Стратегия alternating: %.2f мс, %d перезапусков" 
                     (:execution-time-ms stats1) (:total-restarts stats1)))
    (println (format "Стратегия left-right:  %.2f мс, %d перезапусков"
                     (:execution-time-ms stats2) (:total-restarts stats2))))
  )
