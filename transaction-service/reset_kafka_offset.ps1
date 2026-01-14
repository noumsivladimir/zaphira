# Reset Kafka consumer offset to skip problematic messages
# Run this to skip past the corrupted message at offset 0 in transaction-completed-0

$KAFKA_HOME = "C:\kafka"  # Adjust this to your Kafka installation path
$BOOTSTRAP_SERVER = "192.168.0.122:9092"
$GROUP_ID = "transaction-service-group"
$TOPIC = "transaction-completed"

Write-Host "Resetting Kafka offset for group: $GROUP_ID, topic: $TOPIC" -ForegroundColor Yellow

# Option 1: Reset to latest (skip all old messages)
Write-Host "`nOption 1: Reset to latest (skip all problematic messages)" -ForegroundColor Cyan
$command1 = "& `"$KAFKA_HOME\bin\windows\kafka-consumer-groups.bat`" --bootstrap-server $BOOTSTRAP_SERVER --group $GROUP_ID --topic $TOPIC --reset-offsets --to-latest --execute"

# Option 2: Shift offset by 1 (skip just the current problematic message)
Write-Host "Option 2: Shift offset by 1 (skip current problematic message)" -ForegroundColor Cyan
$command2 = "& `"$KAFKA_HOME\bin\windows\kafka-consumer-groups.bat`" --bootstrap-server $BOOTSTRAP_SERVER --group $GROUP_ID --topic $TOPIC --reset-offsets --shift-by 1 --execute"

Write-Host "`nFirst, stop the transaction-service, then run one of:" -ForegroundColor Green
Write-Host "For Option 1 (recommended): " -ForegroundColor White
Write-Host $command1 -ForegroundColor Gray
Write-Host "`nFor Option 2: " -ForegroundColor White  
Write-Host $command2 -ForegroundColor Gray

Write-Host "`n`nOr manually execute:" -ForegroundColor Yellow
Write-Host "cd $KAFKA_HOME\bin\windows" -ForegroundColor Gray
Write-Host ".\kafka-consumer-groups.bat --bootstrap-server $BOOTSTRAP_SERVER --group $GROUP_ID --reset-offsets --topic $TOPIC --to-latest --execute" -ForegroundColor Gray
