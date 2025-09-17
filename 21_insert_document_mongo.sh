
names=("apple" "banana" "cherry" "mango" "grape" "orange" "peach" "kiwi" "plum" "pear" "melon" "berry" "lemon" "papaya" "guava")
base=${names[$RANDOM % ${#names[@]}]}   # pick random name
rand=$(uuidgen)                          # generate UUID
curl -X POST "http://localhost:8080/entity?name=${base}-${rand}"  