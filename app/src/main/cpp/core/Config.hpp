#pragma once
#include <cstdio>
#include <cstdlib>
#include <stdexcept>
#define PANIC(...) throw std::runtime_error("模拟核心拒绝了此 ROM 或状态")
#define MODEL_DEF_NAME "model.def"
