import React, { useState } from "react";
import {
  Grid,
  TextField,
  FormControlLabel,
  Button,
  Typography,
  Link,
  FormLabel,
  RadioGroup,
  FormControl,
  Radio,
} from "@mui/material";
import { useRegisterUserMutation, useGetUserQuery } from "./api/apiSlice";
import { setToken } from "../userReducer";
import { useDispatch, useSelector } from "react-redux";
import { hasUserTokenSelector } from "../store";

export function LoginView() {
  const [password, setPassword] = useState("testPassword");
  const [username, setUserName] = useState("testUser");
  const [isLogin, setIsLogin] = useState(true);
  const dispatch = useDispatch();
  const hasUserToken = useSelector(hasUserTokenSelector);

  const {
    data: userData,
    isFetching: isFetchingUser,
    error: userError,
  } = useGetUserQuery();
  const [registerUser, { error: registerError }] = useRegisterUserMutation();

  return (
    <Grid>
      <Grid align="center">
        <h2>Sign In</h2>
      </Grid>
      <TextField
        label="Username"
        placeholder="Enter username"
        variant="outlined"
        value={username}
        onChange={(e) => setUserName(e.target.value)}
        fullWidth
        required
      />
      <TextField
        label="Password"
        placeholder="Enter password"
        type="password"
        variant="outlined"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        fullWidth
        required
      />
      <FormControl>
        <FormLabel id="demo-controlled-radio-buttons-group">
          Login Type
        </FormLabel>
        <RadioGroup
          aria-labelledby="demo-controlled-radio-buttons-group"
          name="controlled-radio-buttons-group"
          value={isLogin ? "login" : "register"}
          onChange={(e) => setIsLogin(e.target.value == "login")}
        >
          <FormControlLabel
            value="register"
            control={<Radio />}
            label="Register"
          />
          <FormControlLabel value="login" control={<Radio />} label="Login" />
        </RadioGroup>
      </FormControl>
      <Button
        type="submit"
        color="primary"
        variant="contained"
        onClick={() =>
          registerUser({ username, password, isLogin }).then((data) => {
            dispatch(setToken(data.data));
          })
        }
      >
        {isLogin ? "Login" : "Register"}
      </Button>
      <Typography>
        <Link href="#">Forgot password ?</Link>
      </Typography>
      <Typography>
        {" "}
        Do you have an account ?<Link href="#">Sign Up</Link>
      </Typography>
      <div>
        <div>{userError != undefined ? userError.status : ""}</div>
        <div>
          User when logging in via token:{" "}
          {userData == undefined
            ? "Not loaded yet"
            : userData.name + " - Token: " + hasUserToken}
        </div>
      </div>
    </Grid>
  );
}
