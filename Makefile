# Makefile for Task Management Application Helm Operations

# Variables
CHART_NAME := taskmanager
CHART_PATH := ./helm/taskmanager
NAMESPACE_SIT := taskmanager-sit
NAMESPACE_UAT := taskmanager-uat
NAMESPACE_PROD := taskmanager-prod

# Helm commands
.PHONY: help
help: ## Display this help message
	@echo "Available targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'

.PHONY: lint
lint: ## Lint the Helm chart
	helm lint $(CHART_PATH)

.PHONY: template
template: ## Render chart templates locally
	helm template $(CHART_NAME) $(CHART_PATH) --namespace $(NAMESPACE_SIT)

.PHONY: package
package: ## Package the Helm chart
	helm package $(CHART_PATH)

.PHONY: install-sit
install-sit: ## Install to SIT environment
	helm upgrade --install $(CHART_NAME)-sit $(CHART_PATH) \
		--namespace $(NAMESPACE_SIT) \
		--create-namespace \
		--values $(CHART_PATH)/values-sit.yaml

.PHONY: install-uat
install-uat: ## Install to UAT environment
	helm upgrade --install $(CHART_NAME)-uat $(CHART_PATH) \
		--namespace $(NAMESPACE_UAT) \
		--create-namespace \
		--values $(CHART_PATH)/values-uat.yaml

.PHONY: install-prod
install-prod: ## Install to Production environment
	helm upgrade --install $(CHART_NAME)-prod $(CHART_PATH) \
		--namespace $(NAMESPACE_PROD) \
		--create-namespace \
		--values $(CHART_PATH)/values-prod.yaml

.PHONY: upgrade-sit
upgrade-sit: ## Upgrade SIT deployment
	helm upgrade $(CHART_NAME)-sit $(CHART_PATH) \
		--namespace $(NAMESPACE_SIT) \
		--values $(CHART_PATH)/values-sit.yaml

.PHONY: upgrade-uat
upgrade-uat: ## Upgrade UAT deployment
	helm upgrade $(CHART_NAME)-uat $(CHART_PATH) \
		--namespace $(NAMESPACE_UAT) \
		--values $(CHART_PATH)/values-uat.yaml

.PHONY: upgrade-prod
upgrade-prod: ## Upgrade Production deployment
	helm upgrade $(CHART_NAME)-prod $(CHART_PATH) \
		--namespace $(NAMESPACE_PROD) \
		--values $(CHART_PATH)/values-prod.yaml

.PHONY: uninstall-sit
uninstall-sit: ## Uninstall from SIT environment
	helm uninstall $(CHART_NAME)-sit --namespace $(NAMESPACE_SIT)

.PHONY: uninstall-uat
uninstall-uat: ## Uninstall from UAT environment
	helm uninstall $(CHART_NAME)-uat --namespace $(NAMESPACE_UAT)

.PHONY: uninstall-prod
uninstall-prod: ## Uninstall from Production environment
	helm uninstall $(CHART_NAME)-prod --namespace $(NAMESPACE_PROD)

.PHONY: status-sit
status-sit: ## Check SIT deployment status
	helm status $(CHART_NAME)-sit --namespace $(NAMESPACE_SIT)
	kubectl get pods -n $(NAMESPACE_SIT)

.PHONY: status-uat
status-uat: ## Check UAT deployment status
	helm status $(CHART_NAME)-uat --namespace $(NAMESPACE_UAT)
	kubectl get pods -n $(NAMESPACE_UAT)

.PHONY: status-prod
status-prod: ## Check Production deployment status
	helm status $(CHART_NAME)-prod --namespace $(NAMESPACE_PROD)
	kubectl get pods -n $(NAMESPACE_PROD)

.PHONY: rollback-sit
rollback-sit: ## Rollback SIT to previous version
	helm rollback $(CHART_NAME)-sit --namespace $(NAMESPACE_SIT)

.PHONY: rollback-uat
rollback-uat: ## Rollback UAT to previous version
	helm rollback $(CHART_NAME)-uat --namespace $(NAMESPACE_UAT)

.PHONY: rollback-prod
rollback-prod: ## Rollback Production to previous version
	helm rollback $(CHART_NAME)-prod --namespace $(NAMESPACE_PROD)

.PHONY: history-sit
history-sit: ## Show SIT release history
	helm history $(CHART_NAME)-sit --namespace $(NAMESPACE_SIT)

.PHONY: history-uat
history-uat: ## Show UAT release history
	helm history $(CHART_NAME)-uat --namespace $(NAMESPACE_UAT)

.PHONY: history-prod
history-prod: ## Show Production release history
	helm history $(CHART_NAME)-prod --namespace $(NAMESPACE_PROD)

.PHONY: create-secrets-sit
create-secrets-sit: ## Create secrets for SIT environment (interactive)
	@echo "Creating secrets for SIT environment..."
	@read -p "Enter Database Password: " DB_PASS; \
	read -p "Enter JWT Secret: " JWT_SECRET; \
	kubectl create secret generic $(CHART_NAME)-sit-db-secret \
		--from-literal=DB_PASSWORD=$$DB_PASS \
		--namespace $(NAMESPACE_SIT) --dry-run=client -o yaml | kubectl apply -f -; \
	kubectl create secret generic $(CHART_NAME)-sit-jwt-secret \
		--from-literal=JWT_SECRET=$$JWT_SECRET \
		--namespace $(NAMESPACE_SIT) --dry-run=client -o yaml | kubectl apply -f -

.PHONY: create-secrets-uat
create-secrets-uat: ## Create secrets for UAT environment (interactive)
	@echo "Creating secrets for UAT environment..."
	@read -p "Enter Database Password: " DB_PASS; \
	read -p "Enter JWT Secret: " JWT_SECRET; \
	kubectl create secret generic $(CHART_NAME)-uat-db-secret \
		--from-literal=DB_PASSWORD=$$DB_PASS \
		--namespace $(NAMESPACE_UAT) --dry-run=client -o yaml | kubectl apply -f -; \
	kubectl create secret generic $(CHART_NAME)-uat-jwt-secret \
		--from-literal=JWT_SECRET=$$JWT_SECRET \
		--namespace $(NAMESPACE_UAT) --dry-run=client -o yaml | kubectl apply -f -

.PHONY: create-secrets-prod
create-secrets-prod: ## Create secrets for Production environment (interactive)
	@echo "Creating secrets for Production environment..."
	@read -p "Enter Database Password: " DB_PASS; \
	read -p "Enter JWT Secret: " JWT_SECRET; \
	kubectl create secret generic $(CHART_NAME)-prod-db-secret \
		--from-literal=DB_PASSWORD=$$DB_PASS \
		--namespace $(NAMESPACE_PROD) --dry-run=client -o yaml | kubectl apply -f -; \
	kubectl create secret generic $(CHART_NAME)-prod-jwt-secret \
		--from-literal=JWT_SECRET=$$JWT_SECRET \
		--namespace $(NAMESPACE_PROD) --dry-run=client -o yaml | kubectl apply -f -

.PHONY: dry-run-sit
dry-run-sit: ## Dry run installation for SIT
	helm install $(CHART_NAME)-sit $(CHART_PATH) \
		--namespace $(NAMESPACE_SIT) \
		--values $(CHART_PATH)/values-sit.yaml \
		--dry-run --debug

.PHONY: dry-run-uat
dry-run-uat: ## Dry run installation for UAT
	helm install $(CHART_NAME)-uat $(CHART_PATH) \
		--namespace $(NAMESPACE_UAT) \
		--values $(CHART_PATH)/values-uat.yaml \
		--dry-run --debug

.PHONY: dry-run-prod
dry-run-prod: ## Dry run installation for Production
	helm install $(CHART_NAME)-prod $(CHART_PATH) \
		--namespace $(NAMESPACE_PROD) \
		--values $(CHART_PATH)/values-prod.yaml \
		--dry-run --debug

.PHONY: logs-sit
logs-sit: ## View logs from SIT environment
	kubectl logs -f -l app.kubernetes.io/name=$(CHART_NAME) -n $(NAMESPACE_SIT)

.PHONY: logs-uat
logs-uat: ## View logs from UAT environment
	kubectl logs -f -l app.kubernetes.io/name=$(CHART_NAME) -n $(NAMESPACE_UAT)

.PHONY: logs-prod
logs-prod: ## View logs from Production environment
	kubectl logs -f -l app.kubernetes.io/name=$(CHART_NAME) -n $(NAMESPACE_PROD)
