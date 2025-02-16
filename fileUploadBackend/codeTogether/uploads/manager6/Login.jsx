import React, { useState } from "react";
import axios from "axios";
import { useAuth } from "../../authContext";
import { Link } from "react-router-dom";
import { FaGoogle, FaLinkedin, FaFacebook } from "react-icons/fa";
import { MDBContainer, MDBCol, MDBRow, MDBInput } from "mdb-react-ui-kit";
import "./auth.css";

const Login = () => {
  const [role, setRole] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const { setCurrentUser } = useAuth();
  const [errors, setErrors] = useState({});
  const [showSignUpOptions, setShowSignUpOptions] = useState(false);
  const [isAuthenticated,setIsAuthenticated]=useState(false);

  const toggleSignUpOptions = () => {
    setShowSignUpOptions(!showSignUpOptions);
  };

  const validateUsername = (username) => {
    const isValid = username && username.length >= 3;
    setErrors((prevErrors) => ({
      ...prevErrors,
      username: isValid ? null : "Username must be at least 3 characters long",
    }));
    return isValid;
  };

  const handleLogin = async (e) => {
    e.preventDefault();

    const isValid = validateUsername(role);

    if (!isValid) return;

    try {
      setLoading(true);
      const res = await axios.post("http://localhost:3000/login", {
        email: email,
        password: password,
      });

      localStorage.setItem("token", res.data.token);
      localStorage.setItem("userId", res.data.userId);
      localStorage.setItem("userName", res.data.name);
      
      console.log(res.data)
     if(res.status==200){
      setIsAuthenticated(true);
      console.log(setIsAuthenticated)
      localStorage.setItem("authenticated",isAuthenticated);
      localStorage.setItem("role",res.data.role);
     }
      setCurrentUser(res.data.userId);
      setLoading(false);
    
      if (res.data.role === 'employee') {
        window.location.href = "/empDashboard";
      } else if (res.data.role === "admin") {
        window.location.href = "/home";
      } else if (res.data.role === 'product-manager') {
        window.location.href = "/pmDashboard";
      } else {
        window.location.href = "/"; 
      }
     
    } catch (err) {
      console.error(err);
      alert("Login Failed!");
      setLoading(false);
    }
  };

  return (
    <div>
      
      <MDBContainer fluid className="p-3 my-5">
        <MDBRow className="align-items-center">
          <MDBCol md="6" className="d-none d-md-block">
            <img
              src="https://watermark.lovepik.com/photo/20211124/large/lovepik-business-team-corporate-image-showing-picture_500924136.jpg"
              className="img-fluid rounded-start w-100 h-100 object-fit-cover"
              loading="lazy"
              alt="Corporate Image"
            />
          </MDBCol>
          <MDBCol md="6">
            <div className="signup-form-container">
              <div className="form-box">
                <h1 className="mb-8 text-3xl text-center">Sign In</h1>
                <div className="w-full max-w-lg mx-auto space-y-4">
                  <div className="w-full mb-4">
                    <MDBInput
                      placeholder="Role"
                      type="text"
                      className="block border border-grey-light w-full p-3 rounded"
                      value={role}
                      onChange={(e) => setRole(e.target.value)}
                    />
                    {errors.username && (
                      <p className="text-red-500 text-xs italic">
                        {errors.username}
                      </p>
                    )}
                  </div>
                  <div className="w-full mb-4">
                    <MDBInput
                      placeholder="Email"
                      type="text"
                      className="block border border-grey-light w-full p-3 rounded"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                    />
                  </div>
                  <div className="w-full mb-4">
                    <MDBInput
                      placeholder="Password"
                      type="password"
                      className="block border border-grey-light w-full p-3 rounded"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                    />
                  </div>
                  <div className="w-full">
                    <button
                      type="submit"
                      className="w-full text-center py-3 rounded bg-green-500 text-white hover:bg-green-700 focus:outline-none my-1"
                      onClick={handleLogin}
                    >
                      {loading ? "Loading..." : "Login"}
                    </button>
                  </div>
                </div>
                <div className="text-center text-sm text-grey-dark mt-4">
                  By signing in, you agree to the{" "}
                  <a
                    className="no-underline border-b border-grey-dark text-grey-dark"
                    href="#"
                  >
                    Terms of Service
                  </a>{" "}
                  and{" "}
                  <a
                    className="no-underline border-b border-grey-dark text-grey-dark"
                    href="#"
                  >
                    Privacy Policy
                  </a>
                </div>
                {/* <div className="divider d-flex align-items-center my-4">
                  <p className="text-center fw-bold mx-3 mb-0">OR</p>
                </div> */}
                <div className="social-login-icons">
                  <a href="YOUR_GOOGLE_LOGIN_URL" className="social-icon">
                    <FaGoogle />
                  </a>
                  <a href="YOUR_LINKEDIN_LOGIN_URL" className="social-icon">
                    <FaLinkedin />
                  </a>
                  <a href="YOUR_FACEBOOK_LOGIN_URL" className="social-icon">
                    <FaFacebook />
                  </a>
                </div>
              </div>
              <div className="text-grey-dark mt-6">
                New to Software?{" "}
                <Link className="no-underline border-b border-blue text-blue" to="/contact-us">
                Ask your HR to give Log in credentials
                </Link>
                .
              </div>
            </div>
          </MDBCol>
        </MDBRow>
      </MDBContainer>
    </div>
  );
};

export default Login;
