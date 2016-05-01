#!/bin/sh

SAR=/usr/bin/sar

if [ "`uname -s`" != "HP-UX" ] ; then
        echo "Only For HP-UX"
        exit 1
fi

ARG="u d q b w c a y v m"

usage()
{
        echo "$0: [-f sar filename ]"
        echo ""
        exit 1;
}

while getopts f: i 2>/dev/null
do
        case "$i" in
                f) sarfile="$OPTARG";;
                \?)     usage;;
        esac
done


if [ -n "$OPTIND" ] ; then
        shift `expr $OPTIND - 1`
fi

if [ -z "$sarfile" ] ; then
        sarfile=/var/adm/sa/sa`date +%d`
fi

if [ ! -r "$sarfile" ] ; then
        echo "unable to read : $sarfile ....exiting"
        exit 1
fi

for i in $ARG
do
        $SAR "-${i}" -f $sarfile
        if [ $? != 0 ] ; then exit 1; fi
done

exit 0

