#!/bin/bash
 
### BEGIN INIT INFO
# Provides:             sockd
# Required-Start:       $remote_fs $syslog
# Required-Stop:        $remote_fs $syslog
# Default-Start:        2 3 4 5
# Default-Stop:         0 1 6
# Short-Description:    Start sockd (dante) proxy-server
### END INIT INFO
 
PROGNAME="sockd"
CMD_SOCKD="/usr/local/sbin/sockd"
OPT="-D"
PIDFILE="/var/run/sockd.pid"
. /lib/lsb/init-functions
 
case "$1" in
    start )
        printf "Starting $PROGNAME"
        if start-stop-daemon --start --quiet --oknodo --pidfile $PIDFILE --exec $CMD_SOCKD -- $OPT; then
            log_end_msg 0 || true
        else
            log_end_msg 1 || true
        fi
        ;;
    stop )
        printf "Stoping $PROGNAME"
        if start-stop-daemon --stop --quiet --oknodo --pidfile $PIDFILE; then
            log_end_msg 0 || true
        else
            log_end_msg 1 || true
        fi
        ;;
    restart )
        printf "Restarting $PROGNAME ...\n"
        printf "Stoping $PROGNAME"
        if start-stop-daemon --stop --quiet --oknodo --retry 30 --pidfile $PIDFILE; then
            log_end_msg 0 || true
        else
            log_end_msg 1 || true
        fi
 
        printf "Starting $PROGNAME"
        if start-stop-daemon --start --quiet --oknodo --pidfile $PIDFILE --exec $CMD_SOCKD -- $OPT; then
            log_end_msg 0 || true
        else
            log_end_msg 1 || true
        fi
        ;;
    status )
        status_of_proc -p $PIDFILE $CMD_SOCKD $PROGNAME && exit 0 || exit $?
        ;;
    * )
        printf "Usage: $PROGNAME { start | stop | restart | status }\n"
        exit 1
esac
 
exit 0