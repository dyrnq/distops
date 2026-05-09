[program:registry-${inst.name}]
environment = OTEL_TRACES_EXPORTER=none<#if inst.env??>,${inst.env}</#if>
user = dist
command = /usr/local/bin/registry serve ${app_home}/registry/${inst.name}/config/config.yml
autostart = true
autorestart = true
stdout_logfile = /dev/stdout
stdout_logfile_maxbytes = 0
redirect_stderr = true
stopasgroup = true
killasgroup = true
stopwaitsecs = 30
stopsignal = TERM