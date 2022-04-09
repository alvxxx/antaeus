start:
	docker-compose build pleo-antaeus
	docker-compose up -d pleo-antaeus
	@make wait-app
	docker-compose up -d awslocal

stop:
	docker-compose down

wait-app:
	echo 'Waiting pleo-antaeus to be ready...'
	@until (docker-compose logs --tail 100 pleo-antaeus | grep 'Javalin started'); do >&2 echo "pleo-antaeus is still booting - will wait 5 seconds"; sleep 5; done;
