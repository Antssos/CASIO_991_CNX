#pragma once
#include <vector>
#include <cstdint>
#include <cstring>
#include <stdexcept>
#include <type_traits>
namespace casioemu {
struct StateIO {
 std::vector<uint8_t>& data; bool load; size_t pos=0;
 void bytes(void* v,size_t n) {
 if(load){if(n>data.size()-pos)throw std::runtime_error("状态文件不完整");std::memcpy(v,data.data()+pos,n);}
 else {auto p=(uint8_t*)v; data.insert(data.end(),p,p+n);}pos+=n;
 }
 template<class T> void one(T& v){static_assert(std::is_trivially_copyable<T>::value);bytes(&v,sizeof v);}
 template<class... T> void operator()(T&... v){(one(v),...);}
};
}
