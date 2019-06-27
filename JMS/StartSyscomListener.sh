syscom_listener_pid=`ps -ef |grep java|grep syscom.prop |awk '{print $2}'`

echo $syscom_listener_pid

if [ -z "$syscom_listener_pid" ]
then
   echo "syscom listner not running..."
else
   echo "Stopping syscom listner pid " $syscom_listener_pid
   kill -9 $syscom_listener_pid
fi


. /u01/Middleware/Oracle_Home/user_projects/domains/FLEXCUBE/bin/setDomainEnv.sh
export CLASSPATH=/home/oracle/FCUBS/jmsq:$CLASSPATH
cd /home/oracle/FCUBS/jmsq
nohup java JMS2DB /home/oracle/FCUBS/jmsq/syscom.prop &

tail -f nohup.out


