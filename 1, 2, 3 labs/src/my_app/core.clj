(ns my-app.core)

;; ========== Task C1 ==========
(defn generate-strings [alphabet n]
  (if (<= n 0)
    [""]
    (reduce
     (fn [acc _]
       (mapcat
        (fn [s]
          (map
           (fn [c] (str s c))
           (remove #(= (last s) (first %)) alphabet)))
        acc))
     alphabet
     (range 1 n))))

;; ========== Task C2 ==========
(defn sieve [xs]
  (let [p (first xs)]
    (cons p (lazy-seq (sieve (remove #(zero? (mod % p)) (rest xs)))))))

(def primes (sieve (iterate inc 2)))

;; ========== Task C3 ==========
(defn parallel-filter [pred coll & {:keys [chunk-size]
                                    :or {chunk-size 1000}}]
  (let [chunks (partition-all chunk-size coll)
        futures (doall (map #(future (doall (filter pred %))) chunks))]
    (mapcat deref futures)))

(defn lazy-parallel-filter [pred coll & {:keys [chunk-size prefetch]
                                         :or {chunk-size 1000 prefetch 2}}]
  (let [chunk-seq (partition-all chunk-size coll)
        buffer (java.util.concurrent.LinkedBlockingQueue. prefetch)]

    (future
      (try
        (doseq [chunk chunk-seq]
          (.put buffer (future (doall (filter pred chunk)))))
        (finally
          (.put buffer nil))))

    ((fn next []
       (lazy-seq
        (when-let [fut (.take buffer)]
          (concat (deref fut) (next)))))))