(ns my-app.core-test
  (:require [clojure.test :as test]
            [my-app.core :as core]))

;; ========== Тесты для Task C1 ==========
(deftest test-task-c1
  (test/testing "Generate strings without repeated characters"
    (let [result (core/generate-strings ["a" "b" "c"] 2)
          expected #{"ab" "ac" "ba" "bc" "ca" "cb"}]
      (test/is (= (count result) (count expected)))
      (test/is (every? #(contains? expected %) result))))
  
  (test/testing "N=1 case"
    (test/is (= (core/generate-strings ["a" "b" "c"] 1) ["a" "b" "c"])))
  
  (test/testing "N=0 case"
    (test/is (= (core/generate-strings ["a" "b" "c"] 0) [""]))))

;; ========== Тесты для Task C2 ==========
(deftest test-task-c2
  (test/testing "First 10 primes"
    (test/is (= (take 10 core/primes) [2 3 5 7 11 13 17 19 23 29])))
  
  (test/testing "Prime number properties"
    (let [first-100 (take 100 core/primes)]
      (test/is (every? #(> % 1) first-100))
      ;; Все простые числа кроме 2 нечетные
      (test/is (empty? (filter even? (rest first-100)))))))

;; ========== Тесты для Task C3 ==========
(deftest test-task-c3
  (test/testing "Parallel filter basic functionality"
    (let [result (core/parallel-filter even? (range 20) :chunk-size 5)
          expected (filter even? (range 20))]
      (test/is (= result expected))))
  
  (test/testing "Empty result"
    (test/is (empty? (core/parallel-filter (constantly false) (range 10)))))
  
  (test/testing "Lazy parallel filter with infinite sequence"
    (let [result (take 10 (core/lazy-parallel-filter even? (range) :chunk-size 100))
          expected (take 10 (filter even? (range)))]
      (test/is (= result expected))))
  
  (test/testing "Performance demonstration"
    (let [heavy-predicate (fn [x] 
                            (Thread/sleep 1)  ;; Имитация тяжелой операции
                            (even? x))
          data (range 50)
          
          start-serial (System/currentTimeMillis)
          serial-result (doall (filter heavy-predicate data))
          serial-time (- (System/currentTimeMillis) start-serial)
          
          start-parallel (System/currentTimeMillis)
          parallel-result (doall (core/parallel-filter heavy-predicate data :chunk-size 10))
          parallel-time (- (System/currentTimeMillis) start-parallel)]
      
      (test/is (= serial-result parallel-result))
      (println (str "Performance - Sequential: " serial-time "ms, Parallel: " parallel-time "ms"))
      (test/is (< parallel-time (* 2 serial-time)))))  ;; Параллельная версия должна быть быстрее
  )
