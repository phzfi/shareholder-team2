#!/bin/bash
#See https://askubuntu.com/questions/132059/how-to-make-a-package-manager-wait-if-another-instance-of-apt-is-running
#License CC By-SA 3.0
#Author Radu Rădeanu

function apt_wait {
    i=0
    tput sc
    while fuser /var/lib/dpkg/lock >/dev/null 2>&1 || \
          fuser /var/lib/apt/lists/lock >/dev/null 2>&1 || \
          fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 ;
    do
        case $(($i % 4)) in
            0 ) j="-" ;;
            1 ) j="\\" ;;
            2 ) j="|" ;;
            3 ) j="/" ;;
        esac
        tput rc
        echo -en "\r[$j] Waiting for other software managers to finish..."
        sleep 0.5
        ((i=i+1))
    done
}

echo "Wait for other apt-get to finish"
apt_wait
