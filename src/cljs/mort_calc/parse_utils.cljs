(ns mort-calc.parse-utils)

(defn parse-num [f a]
  (if (not= a "")
    (let [converted (f a)]
      (if (js/isNaN converted)
        0
        converted))
    ""))

(defn parse-int [a]
  (parse-num js/parseInt a))

(defn parse-float [a]
  (parse-num js/parseFloat a))
