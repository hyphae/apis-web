echo 'call start.sh'

CLUSTER_XML=cluster.xml
if [ "$(uname)" = 'Darwin' ] ; then
	CLUSTER_XML=cluster-mac.xml
fi
java -Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Tokyo -Dlogback.configurationFile=./logback.xml -Dvertx.hazelcast.config=./$CLUSTER_XML -jar ../target/apis-web-4.5.10-fat.jar run jp.co.sony.csl.dcoes.apis.tools.web.util.Starter --conf ./config.json --cluster --cluster-host 127.0.0.1

echo '... done'
