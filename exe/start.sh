echo "call start.sh"

java -Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Tokyo -Djava.util.logging.config.file=./logging.properties -jar ../target/apis-web-3.0.0-a01-fat.jar -conf ./config.json -cp ./ -cluster  -cluster-host 127.0.0.1 &

echo "... done"
