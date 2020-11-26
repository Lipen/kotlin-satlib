extern crate jni;
extern crate splr;

use std::convert::TryFrom;

use jni::JNIEnv;
use jni::objects::{JObject, ReleaseMode};
use jni::sys::{jboolean, jint, jintArray, jlong, jsize};
use splr::*;
use splr::types::*;

fn encode(solver: Solver) -> jlong {
    Box::into_raw(Box::new(solver)) as jlong
}

fn decode(solver_ptr: jlong) -> &'static mut Solver {
    unsafe { &mut *(solver_ptr as *mut Solver) }
}

#[no_mangle]
pub extern "system"
fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1hello(
    _env: JNIEnv,
    _object: JObject,
) {
    println!("Hello from Rust!");

    let v: Vec<Vec<i32>> = vec![vec![1, 2], vec![-1, 3], vec![1, -3], vec![-1, 2]];
    for (i, v) in Solver::try_from((Config::default(), v.as_ref())).expect("panic").iter().enumerate() {
        println!("{}-th answer: {:?}", i, v);
    }

    let cnf = CNFDescription {
        num_of_variables: 4,
        ..CNFDescription::default()
    };
    let mut s = Solver::instantiate(&Config::default(), &cnf);
    s.add_clause(vec![1, 2]).unwrap();
    s.add_clause(vec![-2]).unwrap();
    s.inject_assignment(&[-1i32]);
    println!("{:?}", s.validate());

    println!("OK");
}

#[no_mangle]
pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1create(
    _env: JNIEnv,
    _object: JObject,
) -> jlong {
    // let solver = Solver::default();
    let solver = Solver::instantiate(&Config::default(), &CNFDescription::default());
    encode(solver)
}

#[no_mangle]
pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1delete(
    _env: JNIEnv,
    _object: JObject,
    solver_ptr: jlong,
) {
    let _boxed_solver = unsafe { Box::from_raw(solver_ptr as *mut Solver) };
    // Let this boxed solver drop.
}

#[no_mangle]
pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1new_1var(
    _env: JNIEnv,
    _object: JObject,
    solver_ptr: jlong,
) -> jint {
    let solver = decode(solver_ptr);
    solver.add_var() as jint
}

#[no_mangle]
pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1nvars(
    _env: JNIEnv,
    _object: JObject,
    solver_ptr: jlong,
) -> jint {
    let solver = decode(solver_ptr);
    solver.asg.num_vars as jint
}

// #[no_mangle]
// pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1add_1clause__J(
//     _env: JNIEnv,
//     _object: JObject,
//     solver_ptr: jlong,
// ) -> jboolean {
//     let solver = decode(solver_ptr);
//     solver.add_clause(vec![]).is_ok() as jboolean
// }

#[no_mangle]
pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1add_1clause__JI(
    _env: JNIEnv,
    _object: JObject,
    solver_ptr: jlong,
    lit: jint,
) -> jboolean {
    let solver = decode(solver_ptr);
    // solver.add_clause(vec![lit]).is_ok() as jboolean
    solver.add_assignment(lit).is_ok() as jboolean
}

#[no_mangle]
pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1add_1clause__JII(
    _env: JNIEnv,
    _object: JObject,
    solver_ptr: jlong,
    lit1: jint,
    lit2: jint,
) -> jboolean {
    let solver = decode(solver_ptr);
    solver.add_clause(vec![lit1, lit2]).is_ok() as jboolean
}

#[no_mangle]
pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1add_1clause__JIII(
    _env: JNIEnv,
    _object: JObject,
    solver_ptr: jlong,
    lit1: jint,
    lit2: jint,
    lit3: jint,
) -> jboolean {
    let solver = decode(solver_ptr);
    solver.add_clause(vec![lit1, lit2, lit3]).is_ok() as jboolean
}

#[no_mangle]
pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1add_1clause__J_3I(
    env: JNIEnv,
    _object: JObject,
    solver_ptr: jlong,
    literals: jintArray,
) -> jboolean {
    let solver = decode(solver_ptr);
    let len = env.get_array_length(literals).unwrap() as usize;
    let ptr = env
        .get_auto_primitive_array_critical(literals, ReleaseMode::CopyBack)
        .unwrap()
        .as_ptr();
    let vec = unsafe { Vec::from_raw_parts(ptr as *mut jint, len, len) };
    let res = solver.add_clause(&vec);
    std::mem::forget(vec);
    res.is_ok() as jboolean
}

#[no_mangle]
pub extern "system" fn Java_com_github_lipen_satlib_solver_jni_JSplr_splr_1solve(
    env: JNIEnv,
    _object: JObject,
    solver_ptr: jlong,
) -> jintArray {
    let solver = decode(solver_ptr);
    solver.reset();
    if let Ok(Certificate::SAT(model)) = solver.solve() {
        let len = model.len() as jsize;
        let output = env.new_int_array(len).unwrap();
        env.set_int_array_region(output, 0, &model).unwrap();
        output
    } else {
        *JObject::null()
    }
}
