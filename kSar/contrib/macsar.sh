#!/bin/sh


#
# add header to original mac sar
#
# uname -s ; uname -n ; uname -r ; uname -p ; date '+%m/%d/%y'

OS=`uname -s -n -r -p 2>/dev/null`
DT=`date '+%m/%d/%y' 2>/dev/null`
ARG="$*"

lastdashf=0
for i in $*
do
	if [ $lastdashf  == 1 ] ; then file=$i ; fi
	if [ "$i" == "-f" ] ; then lastdashf=1;  else lastdashf=0; fi
done


if [ -n "$file" ] ; then
	D1=`echo $file| sed -e 's/\(.*\)sa\([0-90-9]\)/\2/' 2>/dev/null`
	DT=`date '+%d' 2>/dev/null`
	D2=`date '+%m' 2>/dev/null`
	D3=`date '+%y' 2>/dev/null`
	if [ "$D1" -gt "$DT" ] ; then
		D2=`expr $D2 - 1 2>/dev/null`
		if [ "$D2" -eq "0" ] ; then
			D2=12
			D3=`expr $D3 - 1 2>/dev/null`
		fi
	fi
	DATESTR="$D2/$D1/$D3"
else
	DATESTR="$DT"
fi

echo ""
echo "$OS $DATESTR"
sar $ARG
