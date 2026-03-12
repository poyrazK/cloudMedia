.PHONY: help dev-up dev-down dev-reset

help:
	@printf "Available targets:\n"
	@printf "  make dev-up    Start local infrastructure\n"
	@printf "  make dev-down  Stop local infrastructure (preserve data)\n"
	@printf "  make dev-reset Stop local infrastructure and remove data\n"

dev-up:
	./scripts/dev/start.sh

dev-down:
	./scripts/dev/stop.sh

dev-reset:
	./scripts/dev/reset.sh
